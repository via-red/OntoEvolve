# OntoEvolve SaaS 平台 — 实现计划

> 基于 [saas-architecture.md](saas-architecture.md) 的详细实施方案

---

## 一、模块结构

```
onto-evolve-saas/
├── pom.xml
└── src/
    └── main/
        ├── java/com/ontoevolve/saas/
        │   ├── SaasApplication.java
        │   ├── config/
        │   │   ├── DomainConfig.java          ← 汇总 DTO
        │   │   ├── ConceptConfig.java
        │   │   ├── DecisionTypeConfig.java
        │   │   ├── FeedbackDimensionConfig.java
        │   │   ├── ClassificationRuleConfig.java
        │   │   ├── ConceptMappingConfig.java
        │   │   ├── FieldDefinitionConfig.java
        │   │   └── StoreConfig.java
        │   ├── service/
        │   │   ├── DomainConfigService.java   ← JSON 配置加载
        │   │   ├── DomainCache.java           ← 运行时缓存
        │   │   ├── DomainStoreManager.java    ← Store 注册表
        │   │   ├── DomainOrchestrationService.java
        │   │   └── seed/EducationDomainSeeder.java
        │   ├── controller/
        │   │   ├── AdminDomainController.java
        │   │   └── DomainController.java
        │   ├── store/
        │   │   ├── DomainStore.java           ← Store SPI
        │   │   ├── MemoryDomainStore.java
        │   │   ├── Neo4jDomainStore.java
        │   │   └── DomainStoreFactory.java
        │   └── classifier/
        │       └── ConfigDrivenClassifier.java
        └── resources/
            ├── application.yml
            └── data/
                └── domains/
                    └── education/             ← 教育领域种子配置
                        ├── domain.json
                        ├── ontology.json
                        ├── decisions.json
                        ├── feedback.json
                        ├── rules.json
                        └── fields.json
```

---

## 二、分步实现

### Step 0：项目初始化

**创建模块目录结构和 POM**

`onto-evolve-saas/pom.xml`：

```xml
<project>
    <parent>
        <groupId>com.ontoevolve</groupId>
        <artifactId>ontoevolve-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>
    <artifactId>onto-evolve-saas</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.ontoevolve</groupId>
            <artifactId>onto-evolve-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

**修改根 `pom.xml`**：在 `<modules>` 中添加 `<module>onto-evolve-saas</module>`。

**`SaasApplication.java`**：

```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.ontoevolve.starter",
    "com.ontoevolve.saas"
})
public class SaasApplication {
    public static void main(String[] args) {
        SpringApplication.run(SaasApplication.class, args);
    }
}
```

**`application.yml`**：

```yaml
spring:
  application:
    name: onto-evolve-saas
server:
  port: 8090

onto:
  saas:
    domains-path: data/domains   # JSON 配置目录

  # 以下为框架默认配置（部分会被领域配置覆盖）
  evolution:
    enabled: true
```

---

### Step 1：JSON 配置模型

在 `com.ontoevolve.saas.config` 包下创建配置 POJO。

**核心规则**：
- 所有配置类使用 Jackson 注解，直接映射 JSON 文件
- 使用 `@JsonIgnoreProperties(ignoreUnknown = true)` 保证向前兼容
- 提供合理的默认值

需要创建的 8 个 POJO：

| 类 | JSON 源文件 | 关键字段 |
|----|------------|---------|
| `DomainConfig` | `domain.json` | id, name, namespace, status, llm, store |
| `ConceptConfig` | `ontology.json` | iri, label, parentIri, properties, constraints, sortOrder |
| `DecisionTypeConfig` | `decisions.json` | iri, name, description, paramsSchema, stepsTemplate |
| `FeedbackDimensionConfig` | `feedback.json` | name, label, description, min, max, higherIsBetter, sortOrder |
| `ClassificationRuleConfig` | `rules.json` | type, priority, promptTemplate, keywords, conceptIri, fallbackStrategy |
| `ConceptMappingConfig` | `decisions.json` (内嵌) | conceptIri, decisionIris, autoDerive |
| `FieldDefinitionConfig` | `fields.json` | fieldName, fieldType, label, required, enumValues, sortOrder |
| `StoreConfig` | `domain.json` (内嵌) | type, config (Map) |

再加一个顶层聚合配置：

```java
public class DomainDefinition {
    String id;
    DomainConfig domain;
    List<ConceptConfig> concepts;
    List<DecisionTypeConfig> decisionTypes;
    List<ConceptMappingConfig> conceptMappings;
    List<FeedbackDimensionConfig> feedbackDimensions;
    List<ClassificationRuleConfig> classificationRules;
    List<FieldDefinitionConfig> fieldDefinitions;
}
```

---

### Step 2：配置加载服务

**`DomainConfigService.java`**

```java
@Service
public class DomainConfigService {
    // 构造函数注入：domainsPath（从 application.yml）
    // 核心方法:
    //   loadAllDomains()     — 扫描 domainsPath 目录，加载所有 JSON
    //   loadDomain(id)       — 加载单个领域
    //   saveDomain(id, def)  — 保存领域 JSON
    //   deleteDomain(id)     — 删除领域目录
    //   reloadAll()          — 重新加载配置（热加载入口）
    //
    // 内部实现:
    //   - 扫描 data/domains/{id}/ 目录
    //   - ObjectMapper 读取每个 JSON 文件
    //   - 环境变量替换: ${VAR_NAME} → System.getenv() / System.getProperty()
    //   - 结果缓存到 DomainCache
}
```

**环境变量替换逻辑**：遍历 `store.config` 的 Map value，对字符串类型的值执行 `${ENV_VAR}` → `System.getenv(ENV_VAR)` 替换。

**`DomainCache.java`**

```java
@Component
public class DomainCache {
    private final ConcurrentMap<String, DomainDefinition> cache = new ConcurrentHashMap<>();

    void put(String id, DomainDefinition def) { cache.put(id, def); }
    DomainDefinition get(String id) { return cache.get(id); }
    Map<String, DomainDefinition> getAllPublished() { ... }
    void remove(String id) { cache.remove(id); }
    boolean isPublished(String id) { ... }
}
```

---

### Step 3：数据面 — DomainStore 抽象

**`DomainStore.java`** — 运行时存储 SPI

```java
public interface DomainStore extends PopulationStore {
    // ----- 事件记录 -----
    void saveRecord(InputEvent record);
    List<InputEvent> getRecords(Map<String, Object> filters, int page, int size);
    Optional<InputEvent> getRecord(String recordId);
    long countRecords(Map<String, Object> filters);

    // ----- 反馈 -----
    void saveFeedback(Feedback feedback);
    List<Feedback> getFeedbacks(String recordId);

    // ----- 生命周期 -----
    void initialize(Map<String, Object> config);
    void close();
}
```

`PopulationStore` 已有的方法（`findOrCreatePopulation`, `addMember`, `replaceMembers`, `getPopulation`, `getAllPopulations`, `incrementGeneration`, `incrementFeedbackCounter`, `addTrace`, `getTraces` 等）由 `DomainStore` 继承，同一套实现负责。

**`MemoryDomainStore.java`**

```java
public class MemoryDomainStore extends InMemoryPopulationStore implements DomainStore {
    private final Map<String, InputEvent> records = new ConcurrentHashMap<>();
    private final Map<String, List<Feedback>> feedbacks = new ConcurrentHashMap<>();
    // 实现所有 DomainStore 方法...
}
```

扩展 `InMemoryPopulationStore`（已有种群管理能力）增加记录/反馈存储。

**`Neo4jDomainStore.java`**（后续实现）

```java
public class Neo4jDomainStore implements DomainStore {
    private final Driver neo4jDriver;
    private final String databaseName;
    // 所有操作通过 Cypher 查询执行，USE {databaseName} 切换数据库
}
```

**`DomainStoreFactory.java`**

```java
@Component
public class DomainStoreFactory {
    public DomainStore createStore(StoreConfig storeConfig) {
        return switch (storeConfig.getType()) {
            case "memory" -> new MemoryDomainStore();
            case "neo4j"  -> createNeo4jStore(storeConfig.getConfig());
            default -> throw new IllegalArgumentException("Unknown store type: " + storeConfig.getType());
        };
    }
}
```

**`DomainStoreManager.java`**

```java
@Component
public class DomainStoreManager {
    private final Map<String, DomainStore> stores = new ConcurrentHashMap<>();
    
    void register(String domainId, DomainStore store) { stores.put(domainId, store); }
    DomainStore getStore(String domainId) { ... }
    void unregister(String domainId) { stores.remove(domainId); }
    
    // 启动时自动为所有已发布领域创建 store
    @PostConstruct
    void initializeStores() { ... }
}
```

---

### Step 4：通用编排服务

**`DynamicOntologyService.java`**

```java
@Service
public class DynamicOntologyService {
    // 从 DomainConfig 的概念列表构建运行时 Concept 树
    //
    // 核心方法:
    //   buildConceptTree(domainId)     — 返回树形结构
    //   findConcept(domainId, iri)     — 查找单个概念（含 parent 引用）
    //   findChildren(domainId, iri)    — 查找子概念
    //   getOrCreateConcept(domainId, iri, label, parentIri) — LLM 动态创建
    //   getNestedConcept(domainId, iri) — 沿 parent 链向上查找
    //
    // 概念对象复用核心模型的 Concept 类，
    // properties 从 JSON 的 properties 映射到 Concept.properties
}
```

**`ConfigDrivenClassifier.java`**

```java
@Service
public class ConfigDrivenClassifier {
    // 不是直接实现 Classifier 接口（因为涉及 LLM 调用），
    // 而是由 DomainOrchestrationService 调用
    //
    // classify(domainId, event):
    //   1. 从 DomainCache 加载 domain 的 rules（按 priority 排序）
    //   2. 遍历 rules:
    //      - KEYWORD: event.description 包含 keywords
    //      - LLM: 渲染 promptTemplate，调用 LLMClient
    //   3. 找到匹配 → 用 DynamicOntologyService 解析 Concept
    //   4. 无匹配 → fallbackStrategy
    //      - AUTO_CREATE: 用标签创建新 Concept（存入 JSON? 还是只存内存？先存内存）
    //      - REJECT: 返回 null
    //   5. 返回 Concept（或 null）
}
```

**`DomainOrchestrationService.java`**

```java
@Service
public class DomainOrchestrationService {
    // 构造函数注入：DomainConfigService, DomainStoreManager, 
    //              EvolutionEngine, DynamicOntologyService,
    //              ConfigDrivenClassifier, 各 SPI
    
    // processEvent(domainId, attributes):
    //   1. 校验 attributes 是否符合 fields.json 定义
    //   2. 创建 InputEvent（使用 domainId 前缀的 id）
    //   3. classifier.classify(domainId, event) → Concept
    //   4. 获取该概念的 population（seed if empty）
    //   5. matcher.match() → Assignment
    //   6. 存 record 到 DomainStore
    //   7. 返回 { event, concept, decision }
    
    // submitFeedback(domainId, recordId, scores[]):
    //   1. 按 dimension 数量校验 scores 长度
    //   2. 创建 Execution + Feedback
    //   3. assignment.updateScore(scores)
    //   4. evolutionEngine.recordFeedback(concept)
    //   5. 存 feedback 到 DomainStore
    
    // seedPopulation(domainId, concept, population):
    //   1. 从 DomainCache 获取 concept 的 mappings
    //   2. 对每个 mapped decisionType，创建 Assignment
    //   3. 初始分数随机，generation=0, status=ACTIVE
    
    // findMatchHierarchy(domainId, concept, context):
    //   1. 在当前概念种群中匹配
    //   2. 未匹配 && 有 parent → 递归到 parent
    //   3. 返回 Assignment（或 null）
}
```

---

### Step 5：通用 API 控制器

**`AdminDomainController.java`** — 路径 `/api/admin/domains`

```java
@RestController
@RequestMapping("/api/admin/domains")
public class AdminDomainController {
    // 注入：DomainConfigService, DomainStoreManager
    
    @GetMapping                    → 列出所有领域
    @PostMapping                   → 创建新领域目录 + JSON 文件
    @GetMapping("/{id}")           → 获取领域元信息
    @PutMapping("/{id}")           → 更新 domain.json
    @DeleteMapping("/{id}")        → 删除领域目录
    
    @PostMapping("/{id}/publish")  → status = PUBLISHED + 创建 DomainStore
    @PostMapping("/{id}/unpublish")→ status = DRAFT + 销毁 DomainStore
    
    // 各配置项的独立更新
    @GetMapping("/{id}/ontology")    → 返回 ontology.json 内容
    @PutMapping("/{id}/ontology")    → 覆盖 ontology.json
    // ... decisions, feedback, rules, fields 同理
}
```

**`DomainController.java`** — 路径 `/{domainId}`

```java
@RestController
@RequestMapping("/{domainId}")
public class DomainController {
    // 注入：DomainConfigService, DomainOrchestrationService,
    //      DomainStoreManager, DomainConfigService
    
    // ----- 记录 -----
    @PostMapping("/records")
    public ResponseEntity<?> submitRecord(
            @PathVariable String domainId,
            @RequestBody Map<String, Object> body) {
        validateDomain(domainId);
        validateFields(domainId, body);       // 按 fields.json 校验
        Map<String, Object> result = orchestrationService.processEvent(domainId, body);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/records")
    public ResponseEntity<?> listRecords(
            @PathVariable String domainId,
            @RequestParam Map<String, String> params) {
        DomainStore store = storeManager.getStore(domainId);
        // 分页、过滤
        return ResponseEntity.ok(store.getRecords(filters, page, size));
    }
    
    @GetMapping("/records/{recordId}")
    public ResponseEntity<?> getRecord(
            @PathVariable String domainId,
            @PathVariable String recordId) {
        DomainStore store = storeManager.getStore(domainId);
        return store.getRecord(recordId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/records/{recordId}/feedback")
    public ResponseEntity<?> submitFeedback(
            @PathVariable String domainId,
            @PathVariable String recordId,
            @RequestBody Map<String, Object> body) {
        List<Number> scores = (List<Number>) body.get("scores");
        orchestrationService.submitFeedback(domainId, recordId, scores);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
    
    // ----- 本体 -----
    @GetMapping("/ontology")
    public ResponseEntity<?> getOntology(@PathVariable String domainId) {
        return ResponseEntity.ok(
            dynamicOntologyService.buildConceptTree(domainId));
    }
    
    // ----- 种群 -----
    @GetMapping("/populations")
    public ResponseEntity<?> getPopulations(@PathVariable String domainId) {
        DomainStore store = storeManager.getStore(domainId);
        // 构建种群视图（含 Pareto 前沿）
        return ResponseEntity.ok(buildPopulationView(store));
    }
    
    @GetMapping("/populations/{conceptIri}")
    public ResponseEntity<?> getPopulationDetail(
            @PathVariable String domainId,
            @PathVariable String conceptIri) {
        DomainStore store = storeManager.getStore(domainId);
        DecisionPopulation pop = store.getPopulation(conceptIri).orElse(null);
        // 返回成员列表 + Pareto 点
    }
    
    @PostMapping("/evolve")
    public ResponseEntity<?> triggerEvolution(
            @PathVariable String domainId,
            @RequestBody Map<String, Object> body) {
        String conceptIri = (String) body.get("conceptIri");
        Concept concept = dynamicOntologyService.findConcept(domainId, conceptIri);
        evolutionEngine.runFullEvolution(concept);
        return ResponseEntity.ok(Map.of("status", "evolution_completed"));
    }
    
    // ----- 图谱 -----
    @GetMapping("/graph")
    public ResponseEntity<?> getGraph(
            @PathVariable String domainId,
            @RequestParam(required = false) String conceptIri) {
        // 构建节点+边返回给前端渲染
    }
    
    // ----- 指标 -----
    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics(@PathVariable String domainId) {
        // 从 MetricsCollector + DomainStore 聚合
    }
    
    // ===== 内部辅助 =====
    private void validateDomain(String domainId) {
        DomainDefinition def = domainConfigService.getDomain(domainId);
        if (def == null || def.getDomain().getStatus() != DomainStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found or not published");
        }
    }
    
    private void validateFields(String domainId, Map<String, Object> body) {
        DomainDefinition def = domainConfigService.getDomain(domainId);
        for (FieldDefinitionConfig field : def.getFieldDefinitions()) {
            Object val = body.get(field.getFieldName());
            if (field.isRequired() && val == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field.getFieldName() + " is required");
            }
            // 类型校验...
        }
    }
}
```

---

### Step 6：教育领域种子数据

创建 `data/domains/education/` 目录下的 6 个 JSON 文件，内容参照架构文档中的示例：

| 文件 | 内容来源 |
|------|---------|
| `domain.json` | 从 `education.ttl` 提取命名空间，从 `application.yml` 提取 LLM 配置 |
| `ontology.json` | 从 `education.ttl` 提取概念层次（Behavioral 等 3 类 + 13 个子类） |
| `decisions.json` | 从 `InterventionService.createSeedInterventions()` 提取 6 种干预类型 + mapping |
| `feedback.json` | 从 `Evaluation` 类提取 3 维度（effectiveness, cost, satisfaction） |
| `rules.json` | 从 `classifier.st` 提取 LLM prompt + `LLMActionClassifier` 的 fallback 关键词 |
| `fields.json` | 从 `ActionEvent` 的字段提取（description, severity, location, studentId） |

**`EducationDomainSeeder.java`** — 启动时将 JSON 配置写入 `data/domains/education/`（如果目录不存在）。

---

### Step 7：新建前端项目 `frontend-saas/`

**不修改 `frontend/` 下的任何文件**，在项目根目录新建独立前端项目。

```
frontend-saas/
├── package.json
├── tsconfig.json
├── vite.config.ts
├── index.html
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── api.ts                   ← 调用 SaaS 平台 API
    ├── types/
    │   └── index.ts             ← 类型定义（复用原项目模式）
    ├── pages/
    │   ├── DomainSelector.tsx   ← 首页：显示已发布领域列表
    │   ├── DomainWorkspace.tsx  ← 通用工作区：配置驱动渲染
    │   └── admin/
    │       ├── DomainList.tsx   ← 领域管理列表
    │       └── DomainEditor.tsx ← 领域配置编辑器
    └── components/
        ├── generic/
        │   ├── RecordForm.tsx       ← 通用表单
        │   ├── FeedbackForm.tsx     ← 通用反馈
        │   └── GenericTable.tsx     ← 通用表格
        └── admin/
            ├── OntologyEditor.tsx   ← 概念树编辑器
            └── SchemaEditor.tsx     ← JSON schema 编辑器
```

**技术栈**：与现有 `frontend/` 一致 — Vite 5 + React 18 + TypeScript + React Router v6

**路由设计**：

```
/                    → DomainSelector（显示所有已发布领域）
/admin               → DomainList（领域管理列表）
/admin/domains/{id}  → DomainEditor（编辑领域配置）
/{domainId}          → DomainWorkspace（领域工作区）
```

**与后端的关系**：`frontend-saas` 只调用 `onto-evolve-saas` 的 API（`/api/admin/domains/*` 和 `/{domainId}/*`），不调用 `onto-domain-education` 的 `/api/education/*`。

---

## 三、实现顺序与依赖关系

```
Step 0: 项目初始化
  └── Step 1: JSON 配置模型
        ├── Step 2: 配置加载服务
        │     └── Step 3: DomainStore 抽象
        │           └── Step 4: 通用编排服务
        │                 └── Step 5: 通用 API 控制器
        │                       ├── Step 6: 教育领域种子数据
        │                       └── Step 7: 前端新增页面
        └── （可选）Neo4jDomainStore 实现
```

**每个步骤的验证标准**：

| Step | 验证方法 |
|------|---------|
| Step 0 | `mvn compile` 通过，SaasApplication 启动无报错 |
| Step 1 | 单元测试：Jackson 正确解析 JSON 样例 |
| Step 2 | 启动时自动加载 `data/domains/` 下的配置，`DomainCache.get()` 返回正确值 |
| Step 3 | 创建 MemoryDomainStore → 存入记录 → 取出记录 → 验证一致 |
| Step 4 | 提交事件 → 分类 → 匹配 → 返回决策（使用 education 种子配置） |
| Step 5 | `curl POST /education/records` 返回与现有 API 结构一致的结果 |
| Step 6 | 6 个 JSON 文件就绪，education 领域可正常使用 |
| Step 7 | 前端可选领域 → 操作工作区 → 数据走新 API |

---

## 四、关键设计决策

### 4.1 为什么不直接复用 `EventController` 的代码？

`EventController` 有 1037 行，与教育领域的模型（ActionEvent, ActionType, Intervention）深度耦合。复制出来重构的工作量 > 重新写通用控制器。

### 4.2 种子方案从哪里来？

参考 `InterventionService.createSeedInterventions()` 的逻辑，但改为配置驱动：

```java
private void seedPopulation(String domainId, Concept concept, DecisionPopulation population, DomainDefinition config) {
    List<ConceptMappingConfig> mappings = config.getConceptMappings();
    // 找到匹配当前概念的 mapping（含继承链匹配）
    for (ConceptMappingConfig mapping : findMappings(concept, mappings)) {
        for (String decisionIri : mapping.getDecisionIris()) {
            DecisionTypeConfig dtc = findDecisionType(config, decisionIri);
            if (dtc != null) {
                Decision decision = new Decision(decisionIri, dtc.getName(), dtc.getDescription(), dtc.getStepsTemplate());
                Assignment assignment = new Assignment("seed:" + UUID.randomUUID(), decision, concept, dimensions);
                assignment.setStatus(Assignment.Status.ACTIVE);
                population.addMember(assignment);
            }
        }
    }
}
```

### 4.3 `Concept` 的 IRI 怎么处理？

沿用核心模型的 `Concept` 类，IRI 由 `namespace + 概念名` 拼接：
```
namespace = "http://ontoevolve/education#"
conceptIri = "Behavioral"
full IRI = "http://ontoevolve/education#Behavioral"
```

与现有的 `EducationOntologyService` 生成的 IRI 格式完全一致。

### 4.4 Neo4jDomainStore 何时实现？

第一期先完成 `MemoryDomainStore`，保证核心流程跑通。`Neo4jDomainStore` 作为后续优化项。

---

---

## 六、Docker 部署

### 6.1 `onto-evolve-saas/Dockerfile`

```dockerfile
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY target/*.jar app.jar
COPY data data

EXPOSE 8090

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8090/actuator/health || exit 1

CMD ["sh", "-c", "java -jar app.jar --onto.saas.domains-path=/app/data/domains"]
```

### 6.2 `frontend-saas/Dockerfile`

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 6.3 `frontend-saas/nginx.conf`

```nginx
server {
    listen 80;

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 管理 API
    location /api/ {
        proxy_pass http://saas-backend:8090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 领域 API（/{domainId}/...）
    location ~ ^/([a-zA-Z0-9_-]+)/ {
        proxy_pass http://saas-backend:8090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 6.4 `docker-compose.yml` 新增服务

在现有 `docker-compose.yml` 末尾追加（使用 `saas` profile，与现有服务共存不冲突）：

```yaml
  # === SaaS 平台模式 (add --profile saas) ===
  saas-backend:
    profiles: ["saas"]
    build:
      context: .
      dockerfile: onto-evolve-saas/Dockerfile
    ports:
      - "8090:8090"
    environment:
      DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY}
    volumes:
      - ./data/domains:/app/data/domains

  saas-frontend:
    profiles: ["saas"]
    build: frontend-saas
    ports:
      - "81:80"
    depends_on:
      - saas-backend
```

### 6.5 启动方式

```bash
# 现有教育领域（不变）
docker compose up

# SaaS 平台
docker compose --profile saas up --build

# 全部启动
docker compose --profile saas up -d && docker compose up -d
```

### `domain.json` → `DomainConfig.java`

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public class DomainConfig {
    private String id;
    private String name;
    private String description;
    private String namespace;
    private DomainStatus status = DomainStatus.DRAFT;
    private LlmConfig llm = new LlmConfig();
    private StoreConfig store = new StoreConfig();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LlmConfig {
        private String provider = "openai";
        private String model = "deepseek-v4-flash";
        // getters/setters
    }
    // getters/setters
}
```

### `DomainConfigService.loadAllDomains()`

```java
public Map<String, DomainDefinition> loadAllDomains() {
    File domainsDir = new File(domainsPath);
    if (!domainsDir.exists()) { domainsDir.mkdirs(); return Map.of(); }
    
    Map<String, DomainDefinition> result = new HashMap<>();
    for (File domainDir : domainsDir.listFiles(File::isDirectory)) {
        DomainDefinition def = loadDomain(domainDir);
        if (def != null) result.put(def.getId(), def);
    }
    return result;
}

private DomainDefinition loadDomain(File dir) {
    try {
        DomainDefinition def = new DomainDefinition();
        def.setDomain(objectMapper.readValue(new File(dir, "domain.json"), DomainConfig.class));
        def.setConcepts(loadList(dir, "ontology.json", ConceptConfig[].class, "concepts"));
        def.setDecisionTypes(loadList(dir, "decisions.json", DecisionTypeConfig[].class, "decisionTypes"));
        // ... 其他文件
        resolveEnvVars(def.getDomain().getStore().getConfig());
        return def;
    } catch (IOException e) {
        log.error("Failed to load domain from {}", dir, e);
        return null;
    }
}
```

### `DomainStoreFactory.createStore()`

```java
public DomainStore createStore(StoreConfig storeConfig) {
    return switch (storeConfig.getType()) {
        case "memory" -> new MemoryDomainStore();
        case "neo4j"  -> {
            Map<String, Object> cfg = storeConfig.getConfig();
            String uri = (String) cfg.getOrDefault("uri", "bolt://localhost:7687");
            String database = (String) cfg.getOrDefault("database", "ontoevolve");
            String username = (String) cfg.getOrDefault("username", "neo4j");
            String password = (String) cfg.getOrDefault("password", "");
            Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
            yield new Neo4jDomainStore(driver, database);
        }
        default -> throw new IllegalArgumentException("Unknown store type: " + storeConfig.getType());
    };
}
```
