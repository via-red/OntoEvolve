# OntoEvolve 协作开发指南

> 面向插件开发者与领域应用开发者的完整指南

---

## 一、概述

OntoEvolve 是一个**本体驱动的自进化决策框架**，采用 Maven 多模块 + SPI 插件架构。作为基础框架，它提供了：

- **不可变的核心骨架**（`onto-evolve-core`）：元本体模型 + 进化引擎接口
- **可替换的策略插件**（`onto-evolve-plugins`）：默认实现，可按需覆盖
- **基础设施层**（`onto-evolve-infra`）：LLM 客户端、指标收集、本体验证
- **自动装配层**（`onto-evolve-starter`）：Spring Boot 零代码集成

本文档指导你如何在不修改框架源码的前提下，开发自定义插件和领域应用。

---

## 二、环境搭建

### 前置要求

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 核心开发语言 |
| Maven | 3.8+ | 多模块构建管理 |
| Node.js | 18+ | 前端开发（可选） |
| Docker | 24+ | 运行 Neo4j / PostgreSQL（可选） |

### 克隆与构建

```bash
git clone <repo-url> ontoevolve
cd ontoevolve

# 编译全部模块 + 运行测试
mvn clean verify

# 跳过测试快速编译
mvn clean install -DskipTests

# 启动教育领域示例应用
cd onto-domain-education
mvn spring-boot:run
```

### 模块依赖关系

```
onto-evolve-core          ← 核心抽象，不依赖任何其他模块
    ↑
    ├── onto-evolve-plugins   ← 默认 SPI 实现
    ├── onto-evolve-infra     ← LLM 客户端、验证器、指标
    └── onto-evolve-graph-store ← Neo4j 持久化
          ↑
    onto-evolve-starter      ← Spring Boot 自动装配
          ↑
    onto-domain-education    ← 参考领域应用
```

**你开发的领域应用**只需依赖 `onto-evolve-starter`（它传递性地引入 core、plugins、infra）。如需 Neo4j 持久化，额外引入 `onto-evolve-graph-store`。

---

## 三、插件开发指南

### 3.1 SPI 接口全景

所有可替换策略定义在 `onto-evolve-core` 的 `com.ontoevolve.core.spi` 包中：

| 接口 | 职责 | 触发时机 | 默认实现 |
|------|------|----------|----------|
| `Classifier<I, C>` | 输入事件 → 本体概念分类 | 每个事件到达时 | 无（需领域实现） |
| `Matcher<C, D, A>` | 从种群中选出最佳 Assignment | 每次匹配请求 | `ParetoUCBMatcher` |
| `Variator<D, A>` | 产生新方案（变异） | 宏观进化触发 | `LLMGenerateVariator` 等三种 |
| `Selector<A>` | 多目标 Pareto 选择 | 每次进化 | `ParetoCrowdingSelector` |
| `Migrator<A>` | 跨生态位知识迁移 | 进化后 | `SemanticMigrator` |
| `MetaOptimizer` | 超参数自适应 | 宏观进化后 | `BayesianMetaOptimizer` |
| `CreditAssigner` | 延迟反馈信用分配 | 反馈到达时 | `UniformCreditAssigner` |
| `Environment` | 自动评估方案效果 | 执行后 | `SimulatedEvaluationEnvironment` |
| `PopulationStore` | 种群持久化后端 | 全程 | `InMemoryPopulationStore` |

### 3.2 开发一个自定义插件（以 Selector 为例）

**Step 1：创建实现类**

在你的领域模块中新建一个类，实现目标 SPI 接口：

```java
package com.yourdomain.plugin;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.spi.Selector;
import java.util.*;

/**
 * 自定义选择器：加权随机替代 Pareto 支配排序。
 */
public class WeightedRandomSelector implements Selector<Assignment> {

    @Override
    public List<Assignment> select(List<Assignment> population, int capacity) {
        if (population.size() <= capacity) {
            return new ArrayList<>(population);
        }

        // 按平均分排序并加权采样
        List<Assignment> sorted = population.stream()
                .sorted(Comparator.comparingDouble(Assignment::getAverageScore).reversed())
                .toList();

        List<Assignment> selected = new ArrayList<>();
        double totalWeight = 0;
        for (int i = 0; i < sorted.size(); i++) {
            totalWeight += 1.0 / (i + 1);  // 排名加权
        }

        Random rng = new Random();
        while (selected.size() < capacity) {
            double r = rng.nextDouble() * totalWeight;
            double acc = 0;
            for (int i = 0; i < sorted.size(); i++) {
                acc += 1.0 / (i + 1);
                if (r <= acc && !selected.contains(sorted.get(i))) {
                    selected.add(sorted.get(i));
                    break;
                }
            }
        }
        return selected;
    }
}
```

**Step 2：注册为 Spring Bean**

在你的领域应用配置类中声明 Bean，覆盖默认实现：

```java
package com.yourdomain;

import com.ontoevolve.core.spi.Selector;
import com.yourdomain.plugin.WeightedRandomSelector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyDomainConfig {

    @Bean
    public Selector<?> mySelector() {
        return new WeightedRandomSelector();
    }
}
```

框架的自动装配使用 `@ConditionalOnMissingBean`，检测到你声明了 `Selector` Bean 后，会自动跳过默认的 `ParetoCrowdingSelector`。无需修改框架代码。

**Step 3（可选）：通过 YAML 控制实现切换**

如果你希望保留多个实现并通过配置切换：

```java
@Bean
@ConditionalOnMissingBean(Selector.class)
@ConditionalOnProperty(prefix = "onto.evolution.selector", name = "implementation",
        havingValue = "weighted-random")
public Selector<?> weightedRandomSelector() {
    return new WeightedRandomSelector();
}
```

然后在 `application.yml` 中：

```yaml
onto:
  evolution:
    selector:
      implementation: weighted-random
```

### 3.3 插件开发规范

1. **无状态优先**：插件应设计为无状态或不可变，由 Spring 管理为单例。如需状态，使用 `ConcurrentHashMap` 等线程安全容器。
2. **显式依赖注入**：通过构造函数注入依赖（LLMClient、MetricsCollector 等），不要在插件中自行 new 外部资源。
3. **泛型约束**：实现接口时尽量保留泛型参数，让框架能正确推断类型。
4. **测试覆盖**：每个插件必须有单元测试。参考 `onto-evolve-plugins/src/test/` 中的测试风格。
5. **命名规范**：插件类名采用 `{功能}{算法}Selector/Variator/...` 格式，如 `ParetoCrowdingSelector`。

### 3.4 插件的生命周期

```
框架启动
  │
  ├── 扫描 @Configuration → 注册各类 Bean
  │     ├── 默认 SPI Bean (@ConditionalOnMissingBean)
  │     └── 自定义 SPI Bean（如果声明则覆盖）
  │
  ├── 装配 EvolutionEngine（聚合所有 SPI Bean）
  │
  └── 运行时
        ├── Micro: Feedback → score update (O(1))
        ├── Meso: N条反馈 → Selector 重排序
        └── Macro: 定时/手动 → Variator → Selector → Migrator → MetaOptimizer
```

---

## 四、领域应用开发指南

### 4.1 开发流程总览

```
Step 1          Step 2          Step 3          Step 4          Step 5
┌──────────┐   ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 领域建模  │──►│ 本体定义  │───►│ 实现 SPI  │───►│ 编排服务  │───►│ REST API │
│ 继承模型  │   │ .ttl文件  │    │ Classifier│    │ 组装各环节 │    │ 对外暴露  │
└──────────┘   └──────────┘    └──────────┘    └──────────┘    └──────────┘
      │                              │                                 │
      └──────────────────────────────┴─────────────────────────────────┘
                                       │
                                 Step 6
                              ┌──────────┐
                              │ 配置与测试│
                              │ .yml + test│
                              └──────────┘
```

### 4.2 Step 1：领域建模 — 继承核心元模型

首先确定你的领域概念如何映射到框架元模型：

| 框架元模型 | 你的领域 | 示例（医疗分诊） |
|-----------|----------|-----------------|
| `InputEvent` | 输入事件 | `SymptomReport`（患者主诉） |
| `Concept` | 领域概念 | `TriageCategory`（分诊类别） |
| `Decision` | 领域决策 | `TreatmentPlan`（治疗方案） |
| `Feedback` | 领域反馈 | `Outcome`（治疗效果） |

创建你的领域模型类，继承核心抽象：

```java
// SymptomReport.java — 继承 InputEvent
public class SymptomReport extends InputEvent {
    private final String severity;       // mild / moderate / severe
    private final String bodyArea;       // 身体部位

    public SymptomReport(String id, Instant timestamp, String description,
                         String sourceSystem, String patientId,
                         Map<String, Object> attributes,
                         String severity, String bodyArea) {
        super(id, timestamp, description, sourceSystem, patientId, attributes);
        this.severity = severity;
        this.bodyArea = bodyArea;
    }
    // getters...
}

// TriageCategory.java — 继承 Concept
public class TriageCategory extends Concept {
    private final String colorCode;      // RED / YELLOW / GREEN

    public TriageCategory(String iri, String label,
                          TriageCategory parent, String colorCode) {
        super(iri, label, parent);
        this.colorCode = colorCode;
    }
    // getters...
}

// TreatmentPlan.java — 继承 Decision
public class TreatmentPlan extends Decision {
    private final String department;     // 执行科室

    public TreatmentPlan(String iri, String name, String description,
                         List<String> steps, String department) {
        super(iri, name, description, steps);
        this.department = department;
    }
    // getters...
}

// Outcome.java — 继承 Feedback
public class Outcome extends Feedback {
    public Outcome(String iri, Execution execution,
                   double effectiveness, double timeCost, double patientSatisfaction) {
        super(iri, execution, new double[]{effectiveness, timeCost, patientSatisfaction});
    }
}
```

### 4.3 Step 2：定义领域本体（OWL/TTL）

在 `src/main/resources/ontology/` 下创建你的领域本体文件（如 `medical.ttl`）：

```turtle
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix med: <http://ontoevolve.org/medical#> .

# 分诊类别（TriageCategory）
med:Emergency        rdfs:subClassOf med:TriageCategory ;
                     rdfs:label "急诊" .
med:InternalMedicine rdfs:subClassOf med:TriageCategory ;
                     rdfs:label "内科" .
med:Surgery          rdfs:subClassOf med:TriageCategory ;
                     rdfs:label "外科" .

# 子类别
med:CardiacEmergency rdfs:subClassOf med:Emergency ;
                     rdfs:label "心脏急症" .
med:TraumaEmergency  rdfs:subClassOf med:Emergency ;
                     rdfs:label "外伤急症" .

# 类别互斥约束
med:Emergency owl:disjointWith med:InternalMedicine .
med:Emergency owl:disjointWith med:Surgery .
med:InternalMedicine owl:disjointWith med:Surgery .

# 治疗方案分类
med:Medication       rdfs:subClassOf med:TreatmentPlan ;
                     rdfs:label "药物治疗" .
med:SurgicalProcedure rdfs:subClassOf med:TreatmentPlan ;
                     rdfs:label "手术治疗" .
```

框架通过 Apache Jena 加载 TTL 文件，自动完成推理（subClassOf 传递性、disjointness 冲突检测等）。

### 4.4 Step 3：实现领域 Classifier（唯一必须实现的 SPI）

Classifier 是领域应用中**唯一必须自定义的 SPI**——每个领域的事件语义不同，框架无法提供通用实现。

```java
package com.yourdomain.medical.service;

import com.ontoevolve.core.spi.Classifier;
import com.ontoevolve.infra.llm.LLMClient;
import org.springframework.stereotype.Component;

@Component
public class SymptomClassifier implements Classifier<SymptomReport, TriageCategory> {

    private final LLMClient llmClient;
    private final OntologyService ontologyService;

    public SymptomClassifier(LLMClient llmClient, OntologyService ontologyService) {
        this.llmClient = llmClient;
        this.ontologyService = ontologyService;
    }

    @Override
    public TriageCategory classify(SymptomReport event) {
        // 1. 构建分类 prompt
        String prompt = buildClassificationPrompt(event);

        // 2. 调用 LLM 分类
        String categoryIri = llmClient.classify(prompt);

        // 3. 从本体中查找对应 Concept
        TriageCategory category = ontologyService.findCategory(categoryIri);

        // 4. 层次回退：子类别未找到则回退到父类别
        if (category == null) {
            category = ontologyService.findDefaultCategory();
        }

        return category;
    }

    private String buildClassificationPrompt(SymptomReport event) {
        return """
            请将以下患者症状分类到对应的分诊类别（从本体中选择）：
            主诉：%s
            严重程度：%s
            身体部位：%s

            可用类别：急诊, 内科, 外科
            仅输出类别 IRI。
            """.formatted(event.getRawDescription(), event.getSeverity(), event.getBodyArea());
    }
}
```

### 4.5 Step 4：编写领域编排 Service

参考 `InterventionService` 的模式，创建你的领域编排服务：

```java
@Service
public class TriageService {

    private final OntologyService ontologyService;
    private final EvolutionEngine evolutionEngine;
    private final Matcher<TriageCategory, TreatmentPlan, Assignment> matcher;
    private final Classifier<SymptomReport, TriageCategory> classifier;
    private final MetricsCollector metrics;

    // 构造函数注入（略）

    /**
     * 处理一条分诊事件：分类 → 匹配 → 返回治疗方案
     */
    public TreatmentPlan processReport(SymptomReport report) {
        // Step 1: 分类
        TriageCategory category = classifier.classify(report);

        // Step 2: 获取或创建种群（首次自动播种）
        var population = evolutionEngine.getOrCreatePopulation(category, 15);
        seedIfEmpty(population, category);

        // Step 3: 匹配最佳方案
        var context = new Matcher.Context(report.getSubjectId(),
                Map.of("severity", report.getSeverity()));
        Assignment matched = matcher.match(category, context,
                population.getActiveMembers());

        if (matched != null) {
            return (TreatmentPlan) matched.getDecision();
        }

        // Step 4: 层次回退
        if (category.getParentConcept() instanceof TriageCategory parent) {
            return findMatchInParent(parent, report);
        }
        return null;
    }

    /**
     * 提交治疗效果评估
     */
    public void submitOutcome(Assignment assignment, String patientId,
                              double effectiveness, double timeCost, double satisfaction) {
        Execution execution = new Execution(
                "exec:" + UUID.randomUUID(), assignment, patientId,
                "doctor", Map.of());

        Outcome outcome = new Outcome(
                "outcome:" + UUID.randomUUID(), execution,
                effectiveness, timeCost, satisfaction);

        assignment.updateScore(outcome.getScores());
        evolutionEngine.recordFeedback(assignment.getConcept());
        metrics.recordFeedback();
    }

    private void seedIfEmpty(DecisionPopulation pop, TriageCategory category) {
        if (!pop.getActiveMembers().isEmpty()) return;

        // 基于本体类别创建种子方案
        List<TreatmentPlan> seeds = createSeedsForCategory(category);
        for (TreatmentPlan plan : seeds) {
            Assignment a = new Assignment("seed:" + UUID.randomUUID(),
                    plan, category, 3);
            a.setStatus(Assignment.Status.ACTIVE);
            a.setGeneration(0);
            pop.addMember(a);
        }
    }

    // createSeedsForCategory, findMatchInParent（略）...
}
```

### 4.6 Step 5：暴露 REST API

```java
@RestController
@RequestMapping("/api/medical")
public class TriageController {

    private final TriageService triageService;
    private final EvolutionEngine evolutionEngine;

    public TriageController(TriageService triageService, EvolutionEngine evolutionEngine) {
        this.triageService = triageService;
        this.evolutionEngine = evolutionEngine;
    }

    /** 接收症状报告，返回治疗方案 */
    @PostMapping("/report")
    public ResponseEntity<Map<String, Object>> report(@RequestBody Map<String, Object> body) {
        SymptomReport report = new SymptomReport(
                "rpt:" + UUID.randomUUID(),
                Instant.now(),
                (String) body.get("description"),
                "api",
                (String) body.get("patientId"),
                body,
                (String) body.get("severity"),
                (String) body.get("bodyArea")
        );

        TreatmentPlan plan = triageService.processReport(report);
        return ResponseEntity.ok(Map.of(
                "plan", plan != null ? plan.getName() : "unknown",
                "steps", plan != null ? plan.getSteps() : List.of()
        ));
    }

    /** 提交治疗效果反馈 */
    @PostMapping("/outcome")
    public ResponseEntity<?> outcome(@RequestBody Map<String, Object> body) {
        // 查找 Assignment → 提交 Outcome
        // ...
        return ResponseEntity.ok(Map.of("status", "recorded"));
    }

    /** 手动触发进化 */
    @PostMapping("/evolve/{categoryIri}")
    public ResponseEntity<?> evolve(@PathVariable String categoryIri) {
        evolutionEngine.runFullEvolution(/* concept */);
        return ResponseEntity.ok(Map.of("status", "evolution_started"));
    }

    /** 查看种群状态 */
    @GetMapping("/populations")
    public ResponseEntity<?> populations() {
        return ResponseEntity.ok(evolutionEngine.getPopulations().entrySet().stream()
                .map(e -> Map.of(
                        "concept", e.getKey().getIri(),
                        "size", e.getValue().getActiveMembers().size(),
                        "generation", e.getValue().getGeneration()
                )).toList());
    }
}
```

### 4.7 Step 6：配置 application.yml

```yaml
# src/main/resources/application.yml
spring:
  application:
    name: onto-domain-medical

  # LLM 配置（Spring AI）
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com/v1
      chat:
        options:
          model: deepseek-v4-flash

# OntoEvolve 框架配置
onto:
  rdf:
    ontology-path: classpath:ontology/medical.ttl
    base-namespace: http://ontoevolve.org/medical#
    store-type: tdb2

  graph:
    store-type: memory              # 开发用 memory，生产用 neo4j

  evolution:
    enabled: true
    trigger:
      feedback-count:
        per-niche: 15               # 每15条反馈触发中观进化
    population:
      default-capacity: 20
    variators:
      enabled: true
      weights:
        LLM_GENERATE: 2
        CROSSOVER: 5
        PERTURB: 1
    migration:
      enabled: true
      compatibility-threshold: 0.8

  meta:
    enabled: true

  environment:
    simulated: false                # 生产环境关闭模拟评估

  credit:
    lambda: 0.9
    max-lookback: 100
```

### 4.8 领域应用的 Maven POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ontoevolve</groupId>
        <artifactId>ontoevolve-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>onto-domain-medical</artifactId>

    <dependencies>
        <!-- Starter 传递性引入 core + plugins + infra -->
        <dependency>
            <groupId>com.ontoevolve</groupId>
            <artifactId>onto-evolve-starter</artifactId>
        </dependency>

        <!-- 如需 Neo4j 持久化 -->
        <dependency>
            <groupId>com.ontoevolve</groupId>
            <artifactId>onto-evolve-graph-store</artifactId>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 业务数据库（可选） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 4.9 应用入口

```java
@SpringBootApplication
public class MedicalApplication {
    public static void main(String[] args) {
        SpringApplication.run(MedicalApplication.class, args);
    }
}
```

---

## 五、协作开发规范

### 5.1 Git 工作流

```
main                    ← 稳定分支，随时可发布
  ├── develop           ← 集成开发分支
  │     ├── feature/*   ← 新功能开发
  │     ├── fix/*       ← Bug 修复
  │     └── refactor/*  ← 重构
  └── release/*         ← 发布准备分支
```

**规则：**
- `main` 分支受保护，需通过 PR + Code Review 合并
- 功能分支从 `develop` 切出，合并回 `develop`
- 每个 commit 必须能通过 `mvn verify`（编译 + 测试）
- 提交前确保不包含 `.env`、`credentials.json` 等敏感文件

### 5.2 提交信息规范

采用 `type(scope): description` 格式：

```
feat(core): add crossover variator with dual-parent selection
fix(plugins): correct crowding distance normalization in Pareto selector
docs(guide): add domain app development tutorial
test(education): add ontology validation test for disjoint classes
refactor(starter): extract variator weights to configuration
```

类型：`feat` / `fix` / `docs` / `test` / `refactor` / `chore`

### 5.3 代码审查 Checklist

提交 PR 前自检：

- [ ] 新功能有对应的单元测试，且 `mvn verify` 全部通过
- [ ] 插件实现类有无状态设计（或线程安全）
- [ ] 领域模型正确继承核心元模型类
- [ ] 本体文件 (.ttl) 通过 Jena 加载验证
- [ ] YAML 配置项有合理的默认值
- [ ] 没有硬编码的 API key 或密码
- [ ] 没有引入不必要的第三方依赖
- [ ] 对于新增 SPI 实现，已在配置中声明 `@ConditionalOnMissingBean` 切换逻辑

### 5.4 模块职责边界

| 做 | 在哪里做 | 不在哪里做 |
|----|---------|-----------|
| 定义新 SPI 接口 | `onto-evolve-core` | 不要在 plugins 或 domain 中定义 |
| 实现通用插件 | `onto-evolve-plugins` | 不要依赖具体领域类 |
| 实现领域特定插件 | 你的 `onto-domain-*` | 不要放在 plugins 模块 |
| 添加基础设施 | `onto-evolve-infra` | 不要引入框架层依赖 |
| 修改自动装配 | `onto-evolve-starter` | 不要在 domain 中重复定义 |

### 5.5 测试规范

```
测试金字塔：
  ┌──────────────┐
  │   E2E (少)   │  ← 完整流程测试（如 InterventionService 全链路）
  ├──────────────┤
  │ Integration  │  ← 插件 + EvolutionEngine 组合测试
  ├──────────────┤
  │   Unit (多)  │  ← 每个 SPI 实现独立单元测试
  └──────────────┘
```

- **单元测试**：每个 SPI 实现类至少覆盖核心逻辑 + 边界条件
- **集成测试**：验证插件与 EvolutionEngine 的协作
- **E2E 测试**：验证完整的事件→分类→匹配→反馈循环

参考 `onto-evolve-plugins/src/test/` 和 `onto-domain-education/src/test/` 中的测试范例。

---

## 六、常见问题

### Q1：我的领域应用需要修改框架代码吗？

**不需要。** 框架通过 SPI + `@ConditionalOnMissingBean` 设计支持零侵入扩展。你只需在自己的 Spring 容器中声明同名 Bean，即可覆盖默认实现。

### Q2：我可以定义新的 SPI 接口吗？

可以，但建议先在 `onto-evolve-core` 中定义接口，在 `onto-evolve-plugins` 中提供默认实现。如果只是领域特定扩展，可以在领域模块中定义自己的接口（不在 core 包下）。

### Q3：多个领域应用能同时运行吗？

当前框架设计为**一个 JVM 进程一个领域应用**。如果需要多领域共享基础设施，可以通过微服务部署（每个领域独立实例），共享 Neo4j 和 PostgreSQL 后端。

### Q4：如何调试进化过程？

以下端点可用于观察：
- `GET /api/{domain}/populations` — 查看所有生态位的种群状态
- `GET /api/{domain}/metrics` — 查看进化指标（hypervolume、多样性、LLM 调用成本）
- `GET /api/{domain}/ontology` — 查看本体概念树
- Evolution Engine 日志级别设为 DEBUG 可查看每次变异和选择的详细过程

### Q5：种子方案太少怎么办？

- 增加 `createSeedInterventions()` 中的种子数量（建议每类 4-8 个）
- 降低 `default-capacity`，让种群更快触发进化变异
- 增大 LLM_GENERATE 的 weight，提高主动探索频率
- 启用 `migration` 让跨生态位知识迁移填充稀疏种群

### Q6：如何接入不同的 LLM 提供商？

框架通过 Spring AI 抽象 LLM 调用。只需修改 `application.yml` 中的 Spring AI 配置，即可切换到 OpenAI、Claude、Ollama 等任何兼容的 API：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1
      chat:
        options:
          model: gpt-4o
```

---

## 七、参考资源

| 资源 | 路径 |
|------|------|
| 项目主文档 | `README.md` |
| 架构图 | `docs/v2技术架构.png`、`docs/概念关系图.png` |
| 教育领域示例 | `onto-domain-education/` — 完整参考实现 |
| 插件默认实现 | `onto-evolve-plugins/src/main/java/com/ontoevolve/plugins/` |
| 自动装配配置 | `onto-evolve-starter/.../OntoEvolveAutoConfiguration.java` |
| 前端开发指南 | `docs/frontend-design.md` |
| 环境变量模板 | `.env.example` |
