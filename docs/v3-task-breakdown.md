# OntoEvolve V3 开发任务分解

> **版本**: 0.3.0 (规划)
> **总估算**: 10-15 人周（后端 2 人）
> **前置依赖**: V2 全部功能已完成并稳定
> **优先级策略**: 后端架构改造优先，前端可视化后续迭代
> **兼容策略**: 验证阶段，重干净轻兼容 — SPI 直接替换不保留旧方法，构造器注入不搞 optional

---

## 任务总览

| 阶段 | 范围 | 任务数 | 估算 | 说明 |
|------|------|--------|------|------|
| **P1** | 核心 SPI 定义 + 基础设施 | 8 | 3-4 周 | 全后端：数据模型、SPI、存储、采集、配置 |
| **P2** | 业务逻辑实现 + API | 8 | 4-6 周 | 全后端：Tool/Override/Validator/Variator 具体实现 |
| **P3** | 场景推演完整实现 | 4 | 3-4 周 | 全后端：Scenario SPI、存储、API |
| **P4** | 前端可视化 | 5 | 3-4 周 | 前端，低优先级，可视团队资源安排 |

---

## 一、P1 — 核心 SPI 定义与基础设施

### P1-1: ReasoningTrace 数据模型与 SPI 定义

**目标**: 定义推理链的数据模型和采集接口。

### 涉及文件（新增）

```
onto-evolve-core/src/main/java/com/ontoevolve/core/trace/
├── ReasoningTrace.java                — 推理链聚合根
├── ReasoningStep.java                 — 推理步骤记录（record）
├── ReasoningTraceCollector.java       — 采集器 SPI
├── ReasoningTraceRepository.java      — 存储 SPI
└── TraceConfig.java                   — 配置模型
```

### ReasoningTrace 数据模型

```java
public class ReasoningTrace {
    private String traceId;
    private String eventId;
    private List<ReasoningStep> steps;
    private long totalElapsedMs;
    private Instant createdAt;
}

public record ReasoningStep(
    String stepName,                    // classification | matching | execution | feedback | evolution
    String description,                 // 人类可读描述
    Map<String, Object> inputs,         // 步骤输入
    Map<String, Object> outputs,        // 步骤输出
    Map<String, Object> rationale,      // 决策依据（关键！）
    long elapsedMs,
    List<String> alternatives           // 被放弃的替代方案及原因
) {}
```

### ReasoningTraceCollector SPI

```java
public interface ReasoningTraceCollector {
    void startTrace(String eventId);
    void recordStep(String stepName, ReasoningStep step);
    void endTrace();
    ReasoningTrace getCurrentTrace();
    boolean isEnabled();
}
```

### ReasoningTraceRepository SPI

```java
public interface ReasoningTraceRepository {
    void save(ReasoningTrace trace);
    Optional<ReasoningTrace> findById(String traceId);
    Optional<ReasoningTrace> findByEventId(String eventId);
    Page<ReasoningTraceSummary> findRecent(int page, int size, String detail);
    void archiveBefore(Instant cutoff);
    void deleteOlderThan(Instant cutoff);
}
```

### 验收标准

- [ ] `ReasoningTrace` 数据结构完整，包含全部 5 种步骤类型
- [ ] `ReasoningStep` 使用 record 确保不可变性
- [ ] 两个 SPI 接口 Javadoc 完整，包含使用示例
- [ ] `@ConditionalOnProperty` 控制 bean 创建（enabled=false 时不创建）
- [ ] `TraceConfig` 所有字段有合理默认值，支持 enabled/samplingRate/ttl 等
- [ ] 单元测试：ReasoningTrace 序列化、Config 绑定

### 估算: 2-3 人天 | 开发者: 1 人

---

### P1-2: ReasoningTrace 存储默认实现

**目标**: 实现推理链的默认存储（内存 + 采样器 + 归档服务骨架）。

### 涉及文件（新增）

```
onto-evolve-infra/src/main/java/com/ontoevolve/infra/trace/
├── DefaultReasoningTraceRepository.java  — 默认存储（内存 Map + 采样）
├── TraceSampler.java                     — 采样器（可配置采样率、高价值事件规则）
└── TraceArchiveService.java              — 归档服务骨架（cron 触发，可插拔归档后端）
```

### 采样器逻辑

```java
public class TraceSampler {
    private final double samplingRate;  // 运行时通过 Actuator 可调
    
    public boolean shouldCollectFull(String eventId, String conceptName) {
        if (random.nextDouble() > samplingRate) return false;
        if (isHighValueEvent(eventId)) return true;  // 高价值事件永远采
        return true;
    }
    
    public boolean shouldStoreSummary() {
        return true;  // 摘要 100% 存储
    }
}
```

### 验收标准

- [ ] 默认存储使用 `ConcurrentHashMap`，线程安全
- [ ] 采样器按配置比例采样，高价值事件不受采样率影响
- [ ] 归档服务可配置 cron 表达式，默认实现只打日志（骨架）
- [ ] 采样率可通过 `@RefreshScope` 或 Actuator 动态调整
- [ ] 单元测试：采样器边界条件、并发读写、归档触发逻辑

### 估算: 2-3 人天 | 开发者: 1 人

---

### P1-3: ReasoningTrace 采集桩植入

**目标**: 在现有的事件处理流程中植入采集调用。

### 涉及文件（修改）

```
onto-domain-education/src/main/java/com/ontoevolve/domain/education/service/
└── InterventionService.java           — 植入 5 个采集点
```

### 采集点

| 步骤 | 采集内容 | 位置 |
|------|---------|------|
| **startTrace** | 事件 ID、开始时间 | `processEvent()` 入口 |
| **classification** | LLM 原始响应、每个候选类别的置信度、选中 Concept、推理摘要 | `classify()` 返回后 |
| **matching** | 种群排名、选中 Assignment 的 Pareto 状态、探索/利用标记、层级回退信息 | `match()` 返回后 |
| **execution** | 执行参数、执行者 | `execute()` 返回后 |
| **endTrace** | 总耗时 | `processEvent()` finally 块 |

### 验收标准

- [ ] 5 个采集点全部植入，无遗漏
- [ ] traceCollector 通过依赖注入（`@ConditionalOnBean`），不在类中 new
- [ ] 采集逻辑中无阻塞操作（不写 DB、不调 LLM）
- [ ] 异常路径（`try-catch-finally`）也能正常关闭 trace
- [ ] 性能损耗 < 1%（空实现零开销）

### 估算: 1-2 人天 | 开发者: 1 人

---

### P1-4: ReasoningTrace REST API

**目标**: 提供推理链查询接口，支持三层渐进披露。

### 涉及文件（新增）

```
onto-domain-education/src/main/java/com/ontoevolve/domain/education/controller/
└── TraceController.java               — REST API
```

### API 定义

```java
@RestController
@RequestMapping("/api/trace")
public class TraceController {
    
    @GetMapping("/{eventId}")
    public ResponseEntity<?> getTrace(
        @PathVariable String eventId,
        @RequestParam(defaultValue = "overview") String detail
    ) {
        // detail = summary → 仅摘要（默认，几百字节）
        // detail = overview → 推理链概览（含步骤和关键数据）
        // detail = full     → 完整数据（含 LLM 原始响应等）
    }
    
    @GetMapping("/recent")
    public Page<ReasoningTraceSummary> getRecentTraces(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    );
}
```

### 验收标准

- [ ] 三个 detail 层级返回正确的数据结构
- [ ] trace 不存在时返回 404
- [ ] 分页查询正常工作
- [ ] 单元测试 + Controller 集成测试

### 估算: 1-2 人天 | 开发者: 1 人

---

### P1-5: ReasoningTrace 配置与条件装配

**目标**: 将 ReasoningTrace 集成到 Spring 配置体系。

### 涉及文件（修改）

```
onto-evolve-core/src/main/java/com/ontoevolve/core/config/
└── OntoEvolveConfig.java              — 引用 TraceConfig

onto-evolve-starter/src/main/java/com/ontoevolve/starter/
├── OntoEvolveAutoConfiguration.java   — @ConditionalOnProperty 装配
└── resources/META-INF/spring.factories

onto-evolve-infra/src/main/resources/
└── application-ontoevolve.yml         — 增加 trace 默认配置
```

### YAML 配置

```yaml
onto:
  reasoning-trace:
    enabled: true
    sampling-rate: 0.1
    summary-store: memory
    detail-store: memory
    detail-ttl-days: 90
    archive:
      enabled: true
      cron: "0 3 * * *"
```

### 条件装配逻辑

```java
@Bean
@ConditionalOnProperty(prefix = "onto.reasoning-trace", name = "enabled", 
                        havingValue = "true", matchIfMissing = true)
public ReasoningTraceCollector traceCollector(TraceConfig config) {
    return new DefaultReasoningTraceCollector(config);
}
// enabled=false → bean 不创建 → InterventionService 中 Optional 处理
```

### 验收标准

- [ ] `onto.reasoning-trace.enabled=false` 时不创建 bean
- [ ] 配置变更不需要重启应用
- [ ] 单元测试：条件装配测试、配置绑定测试

### 估算: 1 人天 | 开发者: 1 人

---

### P1-6: HumanOverrideHandler SPI 与默认实现

**目标**: 定义人类修订捕获的 SPI 接口和 diff 策略。

### 涉及文件（新增）

```
onto-evolve-core/src/main/java/com/ontoevolve/core/override/
├── HumanOverrideHandler.java           — SPI 接口
├── OverrideAnalysis.java               — 差异分析报告（record）
├── OverrideAction.java                 — 处理策略枚举
├── DiffStrategy.java                   — diff 策略接口
└── FieldDiffStrategy.java              — 默认字段级 diff
```

### SPI 设计

```java
public interface HumanOverrideHandler<A extends Assignment> {
    OverrideAnalysis analyzeOverride(A original, A actual, String note);
    OverrideAction decideAction(OverrideAnalysis analysis);
}

public record OverrideAnalysis(
    A original,
    A actual,
    Map<String, DiffEntry> diffs,    // 字段名 → 差异详情
    String note,
    double dissimilarity              // [0, 1]
) {}

public record DiffEntry(
    String fieldName,
    Object originalValue,
    Object actualValue,
    DiffType type                     // MODIFIED / ADDED / REMOVED
) {}

public enum OverrideAction {
    REGISTER_AS_NEW,    // 注册为新个体（差异度大）
    UPDATE_EXISTING,    // 更新现有方案（差异度小）
    IGNORE              // 忽略（措辞微调）
}

public interface DiffStrategy {
    Map<String, DiffEntry> computeDiff(Object original, Object actual);
    double computeDissimilarity(Map<String, DiffEntry> diffs);
}
```

### 默认行为

- `FieldDiffStrategy`：对 Decision 的每个字段递归比较
- `dissimilarity` 计算：`修改字段数 / 总字段数`
- `decideAction`：dissimilarity > 0.3 → REGISTER_AS_NEW，> 0.1 → UPDATE_EXISTING，否则 IGNORE

### 验收标准

- [ ] SPI 接口 Javadoc 完整
- [ ] `FieldDiffStrategy` 能正确比较嵌套字段
- [ ] `dissimilarity` 计算在 [0,1] 范围内
- [ ] `decideAction` 阈值可通过参数注入（非硬编码）
- [ ] 单元测试：diff 各种场景（修改/新增/删除/全等）、dissimilarity 边界值、decideAction 阈值

### 估算: 2-3 人天 | 开发者: 1 人

---

### P1-7: Tool 与 ToolRegistry SPI 定义

**目标**: 定义工具注册表的核心接口。

### 涉及文件（新增）

```
onto-evolve-core/src/main/java/com/ontoevolve/core/tool/
├── Tool.java                           — 工具 SPI
├── ToolResult.java                     — 工具执行结果
├── ToolRegistry.java                   — 注册表
├── ToolContext.java                    — 执行上下文
├── ToolExecutionException.java         — 执行异常
└── ToolSandboxConfig.java             — 沙盒配置模型
```

### Tool SPI

```java
public interface Tool {
    String name();                              // 唯一名称，LLM 调用标识
    String description();                       // 描述，注入 prompt
    String parameterSchema();                   // JSON Schema 字符串
    ToolResult execute(JsonNode parameters, ToolContext ctx);
    default boolean isEnabled() { return true; }
}

public class ToolResult {
    public static ToolResult success(JsonNode data, String summary);
    public static ToolResult failure(String error);
    
    private final boolean success;
    private final JsonNode data;
    private final String summary;       // 注入 LLM 的摘要文本（≤200 token）
    private final long elapsedMs;
}

public class ToolRegistry {
    void register(Tool tool);
    void unregister(String toolName);
    Tool getTool(String name);
    List<Tool> getAvailableTools();
    ToolResult execute(String toolName, JsonNode params);
    int toolCount();
}
```

### 验收标准

- [ ] `Tool` SPI Javadoc 完整，含参数 Schema 格式说明
- [ ] `ToolResult` 的 `summary` 字段控制在 200 token 以内
- [ ] `ToolRegistry` 线程安全（`ConcurrentHashMap`）
- [ ] 注册同名工具时抛出 `IllegalArgumentException`
- [ ] 执行不存在的工具时返回 fail 而非抛异常
- [ ] 单元测试：注册/注销/查询/重复注册/非法工具名

### 估算: 1-2 人天 | 开发者: 1 人

---

### P1-8: OntologyValidator SPI 扩展

**目标**: 为现有 OntologyValidator SPI 增加 `validateWithReport` 方法。

### 涉及文件（修改/新增）

```
onto-evolve-core/src/main/java/com/ontoevolve/core/spi/
├── OntologyValidator.java              — 增加 validateWithReport default 方法
└── ValidationReport.java              — 验证报告（新增）
```

### SPI 变更

```java
public interface OntologyValidator {
    /** 替换 V2 的 validate()：获取详细验证报告，包含违反的规则和修正建议 */
    ValidationReport validateWithReport(Assignment assignment, ValidationContext ctx);
}

public class ValidationReport {
    private final boolean passed;
    private final List<RuleViolation> violations;
    private final String correctionPrompt;   // 注入 LLM 的修正引导
    private final int severity;              // 1=建议 2=警告 3=强制
    
    public record RuleViolation(
        String ruleName,
        String ruleDescription,
        String owlAxiom,
        String[] involvedEntities,
        String suggestion
    ) {}
}
```

### 验收标准

- [ ] `OntologyValidator` 接口**直接替换**，V3 不再保留 `validate()` 方法
- [ ] 现有实现（如 `EducationOntologyValidator`）同步更新为新签名
- [ ] 单元测试：全部通过、部分违反、严重违反

### 估算: 1 人天 | 开发者: 1 人

---

## 二、P2 — 业务逻辑实现与 API

### P2-1: SandboxedToolExecutor 实现

**目标**: 实现工具安全执行沙盒（参数校验、超时隔离、熔断）。

### 涉及文件（新增）

```
onto-evolve-core/src/main/java/com/ontoevolve/core/tool/
├── SandboxedToolExecutor.java          — 安全执行器
└── CircuitBreaker.java                 — 熔断器
```

### 安全执行流程

```java
public class SandboxedToolExecutor {
    private final long timeoutMs;
    private final long maxResultBytes;
    private final int maxCallsPerVariation;
    private final CircuitBreaker circuitBreaker;
    
    public ToolResult executeSafely(Tool tool, JsonNode params, String variationId) {
        // 1. 熔断检查
        if (circuitBreaker.isOpen(tool.name())) 
            return ToolResult.failure("工具 " + tool.name() + " 已熔断");
        
        // 2. JSON Schema 参数校验
        if (!validateParams(tool.parameterSchema(), params))
            return ToolResult.failure("参数校验失败");
        
        // 3. 单次变异调用频次控制
        if (callCounter.increment(variationId) > maxCallsPerVariation)
            return ToolResult.failure("超过单次变异调用上限");
        
        // 4. 隔离执行（超时控制）
        CompletableFuture<ToolResult> future = CompletableFuture
            .supplyAsync(() -> tool.execute(params, ctx))
            .orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        
        try {
            ToolResult result = future.get();
            result = sanitize(result);        // 大小限制 + NaN 过滤
            circuitBreaker.recordSuccess(tool.name());
            return result;
        } catch (TimeoutException e) {
            circuitBreaker.recordError(tool.name());
            return ToolResult.failure("执行超时");
        } catch (Exception e) {
            circuitBreaker.recordError(tool.name());
            return ToolResult.failure("执行异常: " + e.getMessage());
        }
    }
}
```

### 验收标准

- [ ] JSON Schema 校验拦截类型错误和注入尝试
- [ ] 工具超时后返回 fail，不抛异常到调用方
- [ ] 结果大小超限时拒绝，NaN/Infinity 被过滤
- [ ] 熔断器连续 errorThreshold 次错误后打开，resetTimeoutMs 后半开
- [ ] 单次变异调用频次控制正确
- [ ] 单元测试：超时模拟、异常模拟、熔断状态机、参数注入

### 估算: 2-3 人天 | 开发者: 1 人

---

### P2-2: 内置工具实现

**目标**: 实现 3 个开箱即用的内置工具。

### 涉及文件（新增）

```
onto-evolve-plugins/src/main/java/com/ontoevolve/plugins/tool/
├── PredictEffectTool.java              — KNN 效果预测
├── SimilarCasesTool.java               — Neo4j 历史案例查询
└── CalculateRiskTool.java              — 规则风险评分

onto-evolve-plugins/src/main/resources/tool-schemas/
├── predict-effect-schema.json
├── similar-cases-schema.json
└── calculate-risk-schema.json
```

### PredictEffectTool

```java
public class PredictEffectTool implements Tool {
    private final PopulationStore populationStore;
    private final int kNeighbors = 5;
    
    @Override
    public ToolResult execute(JsonNode params, ToolContext ctx) {
        List<HistoricalRecord> history = loadHistory(params.get("concept").asText());
        
        if (history.size() < 3)
            return ToolResult.success(
                lowConfidenceResponse(history.size()),
                "历史数据不足 (n=" + history.size() + "), 预测置信度低");
        
        List<HistoricalRecord> neighbors = findNearestNeighbors(
            params.get("intervention"), history, kNeighbors);
        
        double predicted = weightedAverage(neighbors);
        double confidence = calculateConfidence(neighbors, history.size());
        
        return ToolResult.success(
            jsonResult(predicted, confidence),
            String.format("预测效果: %.2f (置信度: %.2f)", predicted, confidence));
    }
}
```

### SimilarCasesTool（Neo4j Cypher 查询）

```cypher
MATCH (c:ActionType {name: $concept})
MATCH (c)<-[:FOR_CONCEPT]-(a:Assignment)
MATCH (a)-[:DECIDES]->(i:Intervention)
MATCH (a)<-[:EXECUTES]-(e:Execution)
MATCH (e)<-[:EVALUATES]-(ev:Evaluation)
RETURN i.name, ev.effectiveness, ev.cost, ev.satisfaction, e.executedAt
ORDER BY ev.effectiveness DESC
LIMIT $topN
```

### CalculateRiskTool（规则驱动）

```java
public class CalculateRiskTool implements Tool {
    private final List<RiskRule> riskRules = List.of(
        new RiskRule("COST_EXCEEDS_LIMIT", "成本超过阈值", 2, 
            params -> params.get("cost").asDouble() > 100.0),
        new RiskRule("INCOMPATIBLE_CONCEPT", "方案类型与概念不兼容", 3,
            params -> checkCompatibility(params))
    );
    
    @Override
    public ToolResult execute(JsonNode params, ToolContext ctx) {
        List<RiskAlert> alerts = riskRules.stream()
            .filter(r -> r.matches(params))
            .map(r -> new RiskAlert(r.name(), r.severity(), r.explanation()))
            .toList();
        return ToolResult.success(jsonResult(alerts), summary(alerts));
    }
}
```

### 验收标准

- [ ] `predict_effect` 在历史数据 < 3 条时返回低置信度标记
- [ ] `predict_effect` 预测值在 [0,1] 范围内
- [ ] `similar_cases` 查询语法正确，Neo4j 集成测试通过
- [ ] `calculate_risk` 至少内置 3 条规则
- [ ] 每个工具带 JSON Schema 校验文件
- [ ] summary 字段不超过 200 token
- [ ] 三个工具都支持 `@ConditionalOnMissingBean` 覆盖

### 估算: 4-5 人天 | 开发者: 1 人

---

### P2-3: Tool Registry 集成 Variator

**目标**: 让 Variator 在生成变异时调用注册工具，注入 prompt。

### 涉及文件（修改/新增）

```
onto-evolve-plugins/src/main/java/com/ontoevolve/plugins/variator/
├── AbstractToolAwareVariator.java      — 工具感知基类（新增）
├── LLMGenerateVariator.java            — 继承基类，增强 prompt
└── CrossoverVariator.java              — 继承基类，增强 prompt
```

### Prompt 模板变更

`variator_generate.st` 增加工具描述注入点：

```st
可用工具（调用它们获取数据支撑决策）：
{toolDescriptions}

注意：先调用工具获取数据，再基于结果撰写方案。
工具调用后你会收到 [[TOOL_RESULT: ...]]，请据此生成方案。
```

### 工具调用处理

```java
public abstract class AbstractToolAwareVariator {
    private final ToolRegistry toolRegistry;
    private final SandboxedToolExecutor toolExecutor;
    
    protected AbstractToolAwareVariator(ToolRegistry toolRegistry, SandboxedToolExecutor toolExecutor) {
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
    }
    
    protected String processToolCalls(String llmResponse, String variationId) {
        for (ToolCall call : parseToolCalls(llmResponse)) {
            ToolResult result = toolExecutor.executeSafely(
                toolRegistry.getTool(call.name()), call.params(), variationId);
            llmResponse = llmResponse.replace(
                call.originalText(),
                "[[TOOL_RESULT: " + result.summary() + "]]");
            recordToolCallToTrace(call, result);
        }
        return llmResponse;
    }
}
```

### 验收标准

- [ ] ToolRegistry/SandboxedToolExecutor 通过构造器注入，非 optional
- [ ] 工具调用结果正确注入 LLM 二次生成上下文
- [ ] 工具执行结果写入 EvolTrace
- [ ] 工具调用次数不超过 maxCallsPerVariation
- [ ] 单元测试 + 集成测试：工具调用全链路

### 估算: 3-4 人天 | 开发者: 1 人

---

### P2-4: Tool 执行 Metrics

**目标**: 为工具执行添加可观测性。

### 涉及文件（修改）

```
onto-evolve-infra/src/main/java/com/ontoevolve/infra/metrics/
├── MetricsCollector.java               — 增加工具指标
└── ToolMetricsExporter.java            — 工具指标导出（新增）
```

### 指标

| 指标名 | 类型 | 标签 |
|--------|------|------|
| `tool.calls.total` | Counter | tool_name, status |
| `tool.calls.latency` | Timer | tool_name |
| `tool.circuit.breaker.state` | Gauge | tool_name |
| `tool.sandbox.rejections` | Counter | reason |

### 验收标准

- [ ] 4 个指标全部正确采集
- [ ] Micrometer/Prometheus 格式兼容
- [ ] 指标带有正确的标签

### 估算: 1 人天 | 开发者: 1 人

---

### P2-5: Tool Registry 集成测试

**目标**: 验证 Tool Registry + Sandbox + Variator 端到端正确性。

### 测试清单

| 测试 | 类型 | 说明 |
|------|------|------|
| Tool 注册/查询/执行 | 单元 | 基础功能 |
| Sandbox 参数校验拦截 | 单元 | 注入非法参数 |
| Sandbox 超时拦截 | 单元 | mock 慢工具 |
| Sandbox 熔断器 | 单元 | 连续错误后熔断 |
| Variator 无工具模式 | 集成 | toolRegistry=null，行为与 V2 一致 |
| Variator 有工具模式 | 集成 | 工具注入 + 二次生成 |
| 多工具调用 | 集成 | 单次变异调用多个工具 |

### 验收标准

- [ ] 7 个测试全通过
- [ ] 验证：单次变异正确调用工具并获取结果

### 估算: 2-3 人天 | 开发者: 1 人

---

### P2-6: JenaOntologyValidator 增强

**目标**: 在 JenaOntologyValidator 中实现 `validateWithReport`。

### 涉及文件（修改）

```
onto-evolve-infra/src/main/java/com/ontoevolve/infra/validation/
├── JenaOntologyValidator.java          — 增加 validateWithReport 实现
└── RuleExtractor.java                  — OWL 规则提取器（新增）
```

### 实现逻辑

```java
@Override
public ValidationReport validateWithReport(Assignment assignment, ValidationContext ctx) {
    List<RuleViolation> violations = new ArrayList<>();
    
    // 1. OWL 类存在性检查（带祖先回溯）
    checkClassExistence(assignment).ifPresent(error -> violations.add(
        new RuleViolation("OWL_CLASS_EXISTENCE", error, "...",
            new String[]{assignment.getConcept().getIri()},
            "建议使用已有概念或先在本体中注册新概念")));
    
    // 2. Disjointness 检查（传递闭包）
    violations.addAll(checkDisjointness(assignment));
    
    // 3. 模型一致性检查
    violations.addAll(checkConsistency(ctx));
    
    // 4. 生成 correctionPrompt
    String correctionPrompt = buildCorrectionPrompt(violations);
    
    return new ValidationReport(violations.isEmpty(), violations, 
        correctionPrompt, maxSeverity(violations));
}

private String buildCorrectionPrompt(List<RuleViolation> violations) {
    if (violations.isEmpty()) return "";
    StringBuilder sb = new StringBuilder("你生成的方案被以下规则拒绝，请修正后重新生成：\n\n");
    for (RuleViolation v : violations) {
        sb.append("- 规则: ").append(v.ruleDescription()).append("\n");
        sb.append("  建议: ").append(v.suggestion()).append("\n\n");
    }
    sb.append("请在保持原有意图的前提下，修改方案使其符合上述规则。");
    return sb.toString();
}
```

### 验收标准

- [ ] 至少提取 3 类规则（类存在性、disjointness、一致性）
- [ ] `correctionPrompt` 可直接注入 LLM prompt
- [ ] 全部通过时返回 `ValidationReport.passed()`
- [ ] 单元测试：全部通过、部分违反、严重违反

### 估算: 2-3 人天 | 开发者: 1 人

---

### P2-7: Rule-Guided 重试循环

**目标**: 在 Variator 中集成规则验证 + 注入修正 prompt 的重试。

### 涉及文件（新增）

```
onto-evolve-plugins/src/main/java/com/ontoevolve/plugins/variator/
├── RuleInjectionPromptBuilder.java     — 修正 prompt 构建器
└── VariatorRetryConfig.java           — 重试配置
```

### 重试逻辑

```yaml
onto:
  variation:
    rule-guided-retry:
      max-attempts: 2                  # 最多重试 2 次
      temperature-decay: 0.2           # 每次重试降创造力
      backoff-ms: 500
      fallback-behavior: BYPASS        # 超上限后绕过
    metrics:
      violation-rate-alert-threshold: 0.3
```

```java
protected Assignment generateWithRuleGuidance(String basePrompt, VariationContext ctx) {
    int attempts = 0;
    double temperature = ctx.getTemperature();
    
    while (attempts <= retryConfig.getMaxAttempts()) {
        attempts++;
        Assignment candidate = parseAssignment(callLLM(basePrompt, temperature));
        
        ValidationReport report = ontologyValidator.validateWithReport(candidate, ctx);
        if (report.passed()) return candidate;
        
        temperature = Math.max(temperature - 0.2, 0.1);
        basePrompt += "\n\n" + report.correctionPrompt();
    }
    return fallbackBehavior(ctx);  // 重试耗尽，按配置处理
}
```

### 验收标准

- [ ] 不通过时正确注入修正 prompt 并重试
- [ ] 每次重试降低 temperature
- [ ] 超过上限后按 fallbackBehavior 处理
- [ ] 违反率记录到 MetricsCollector
- [ ] 违反率 > 30% 触发告警日志
- [ ] 单元测试：直接通过、一次重试通过、重试耗尽三种场景

### 估算: 2-3 人天 | 开发者: 1 人

---

### P2-8: Assignment 模型变更 + Override API

**目标**: 实现人类修订的模型变更和 REST API。

### 涉及文件（修改/新增）

```
onto-evolve-core/src/main/java/com/ontoevolve/core/model/
└── Assignment.java                     — 增加 HUMAN_OVERRIDE/SUPERSEDED 状态 + 字段

onto-evolve-plugins/src/main/java/com/ontoevolve/plugins/override/
└── DefaultHumanOverrideHandler.java    — SPI 默认实现

onto-domain-education/src/main/java/com/ontoevolve/domain/education/
├── service/InterventionService.java    — 增加 processOverride 方法
└── controller/EventController.java     — 增加 /education/override 端点
```

### Assignment 变更

```java
public enum AssignmentStatus {
    NEWBORN, ACTIVE, PROBATION, DEPRECATED, ELITE,
    HUMAN_OVERRIDE,   // 新增：被人类改写
    SUPERSEDED        // 新增：已被新方案替代
}

public class Assignment {
    // ... 现有字段
    private String humanOverrideNote;
    private String supersededBy;  // 替代者的 assignment ID
}
```

### REST API

```java
@PostMapping("/education/override")
public ResponseEntity<?> submitOverride(@RequestBody OverrideRequest request) {
    // 1. 查找原始 Assignment
    // 2. HumanOverrideHandler 分析差异
    // 3. 按 OverrideAction 处理（REGISTER_AS_NEW/UPDATE_EXISTING/IGNORE）
    // 4. 原始 Assignment 标记 SUPERSEDED
    // 5. 记录到 ReasoningTrace
}
```

### 验收标准

- [ ] `POST /education/override` 端点正常工作
- [ ] REGISTER_AS_NEW 时新 Assignment 继承父代评分向量
- [ ] 新 Assignment 的 parent 指向原始方案
- [ ] 原始 Assignment 标记 SUPERSEDED
- [ ] 输入校验（eventId/assignmentId 存在性）
- [ ] 单元测试 + 集成测试

### 估算: 2-3 人天 | 开发者: 1 人

---

## 三、P3 — 场景推演完整实现

### P3-1: Scenario SPI 定义

**目标**: 定义场景推演的核心 SPI。

### 涉及文件（新增）

```
onto-evolve-core/src/main/java/com/ontoevolve/core/scenario/
├── ScenarioContext.java                — 分支上下文（含 baselineGeneration）
├── ScenarioOrchestrator.java           — 推演编排器 SPI
├── ComparisonReport.java               — 对比报告（使用 delta）
├── MergeStrategy.java                  — 合并策略接口
├── SnapshotStrategy.java               — 快照策略接口
└── ScenarioStore.java                  — 分支存储 SPI
```

### 核心 SPI

```java
public interface ScenarioOrchestrator {
    ScenarioContext createBranch(String name, String conceptId);
    ScenarioResult runScenario(ScenarioContext ctx, ScenarioAction action);
    ComparisonReport compareResults(String branchA, String branchB);
    MergeResult mergeBranch(String source, String target, MergeStrategy strategy);
    void discardBranch(String name);
    List<ScenarioSummary> listBranches();
}

public class ComparisonReport {
    private final int baselineGeneration;       // 分支创建时的主线代际
    private final double branchDelta;           // 分支从基准到现在的指标变化
    private final double mainlineDelta;         // 主线同期的指标变化（估计值）
    // ... 
}
```

### 验收标准

- [ ] SPI 接口 Javadoc 完整
- [ ] `ComparisonReport` 使用 delta 而非绝对值
- [ ] `ScenarioContext` 包含 baselineGeneration
- [ ] 单元测试：对比计算、合并决策逻辑

### 估算: 2-3 人天 | 开发者: 1 人

---

### P3-2: InMemoryScenarioStore 实现

**目标**: 基于写时复制的分支存储。

### 涉及文件（新增）

```
onto-evolve-plugins/src/main/java/com/ontoevolve/plugins/scenario/
├── InMemoryScenarioStore.java          — 写时复制
└── PopulationSnapshotStrategy.java     — 快照策略（含规模自适应）
```

### 验收标准

- [ ] 分支上的修改不影响主线（写时复制）
- [ ] 快照策略根据种群规模自动选择（全量/Pareto前沿+摘要/仅摘要）
- [ ] 分支存储支持完整的 PopulationStore 操作
- [ ] 单元测试：快照隔离性、多策略切换

### 估算: 3-4 人天 | 开发者: 1 人

---

### P3-3: Scenario REST API

**目标**: 场景推演的 REST 接口。

### 涉及文件（新增）

```
onto-domain-education/src/main/java/com/ontoevolve/domain/education/controller/
└── ScenarioController.java
```

### API

```java
POST   /api/scenario/branch                — 创建分支
POST   /api/scenario/branch/{name}/run     — 执行推演
GET    /api/scenario/compare?b1=X&b2=Y     — 对比
POST   /api/scenario/merge?from=X&to=Y     — 合并
DELETE /api/scenario/branch/{name}         — 丢弃
GET    /api/scenario/branches              — 列表
```

### 验收标准

- [ ] 6 个端点全部实现
- [ ] 对比报告使用 delta
- [ ] 错误处理：404/409 正确返回

### 估算: 1-2 人天 | 开发者: 1 人

---

### P3-4: Scenario 集成测试

**目标**: 验证场景推演端到端链路。

### 测试清单

| 测试 | 说明 |
|------|------|
| 创建分支 → 执行推演 → 对比 | 完整推演链路 |
| 分支不影响主线 | 写时复制验证 |
| 合并到主线 | 种群状态正确更新 |
| 丢弃分支 | 资源释放 |
| 快照策略选择 | 不同规模选择不同策略 |

### 估算: 1-2 人天 | 开发者: 1 人

---

## 四、P4 — 前端可视化（低优先级）

以下任务为前端可视化，建议放在架构改造完成后、视团队资源安排。

| 任务 | 涉及页面 | 依赖 API | 估算 | 优先级 |
|------|---------|---------|------|--------|
| P4-1: 推理链展示 | Events 页展开详情 | `/api/trace/{eventId}` | 5-7 人天 | ⭐⭐ |
| P4-2: Override 表单 | Events 页执行表单 | `/education/override` | 3-4 人天 | ⭐⭐ |
| P4-3: Tool Metrics 面板 | Dashboard | `/api/metrics/tools` | 3-4 人天 | ⭐ |
| P4-4: 违规统计卡片 | Dashboard | `/api/metrics/violations` | 2-3 人天 | ⭐ |
| P4-5: 场景推演页面 | 新页面 Scenario | `/api/scenario/*` | 5-7 人天 | ⭐⭐ |

**前端任务详细设计** 见 `docs/v3-frontend-tasks.md`（视需要另行创建）。

---

## 五、依赖关系图（后端部分）

```
P1-1 Trace SPI ─────────────┐
       │                    │
       ├─→ P1-2 存储实现 ──→┤
       │                    │
       ├─→ P1-3 采集桩植入 ─→┤
       │                    ├──→ P1-4 Trace API
       │                    │
       ├─→ P1-5 配置装配 ──→┤
       │                    │
P1-6 Override SPI ─────────┘
       │
       ├──────────────────────→ P2-8 Override API
       │
P1-7 Tool SPI ───────→ P2-1 Sandbox ────→ P2-3 Variator集成
       │                      │                │
       └──→ P2-2 内置工具 ────┘                │
                                                ├──→ P2-5 集成测试
P1-8 Validator SPI ──→ P2-6 Jena增强 ──→ P2-7 重试循环
                                               │
P3-1 Scenario SPI ──→ P3-2 存储实现 ──→ P3-3 API ──→ P3-4 测试
```

### 关键路径

**P1（3-4 周）**: P1-1 → P1-2 → P1-3 → P1-4，P1-7 → P2-1（可并行）
**P2（4-6 周）**: P2-1 → P2-3 → P2-5 + P2-6 → P2-7（P2-2/P2-4/P2-8 可并行）
**P3（3-4 周）**: P3-1 → P3-2 → P3-3 → P3-4

---

## 六、文件变更汇总

### 新增文件（后端仅核心）

```
onto-evolve-core/src/main/java/com/ontoevolve/core/trace/        (6 个)
onto-evolve-core/src/main/java/com/ontoevolve/core/tool/          (6 个)
onto-evolve-core/src/main/java/com/ontoevolve/core/override/      (5 个)
onto-evolve-core/src/main/java/com/ontoevolve/core/scenario/      (6 个)
onto-evolve-infra/src/main/java/com/ontoevolve/infra/trace/       (3 个)
onto-evolve-infra/src/main/java/com/ontoevolve/infra/metrics/     (1 个)
onto-evolve-plugins/src/main/java/com/ontoevolve/plugins/tool/    (3 个)
onto-evolve-plugins/src/main/java/com/ontoevolve/plugins/override/ (1 个)
onto-evolve-plugins/src/main/java/com/ontoevolve/plugins/variator/ (3 个)
onto-evolve-plugins/src/main/java/com/ontoevolve/plugins/scenario/ (2 个)
onto-domain-education/.../controller/TraceController.java         (1 个)
onto-domain-education/.../controller/ScenarioController.java      (1 个)
```

### 修改文件

```
onto-evolve-core/src/main/java/com/ontoevolve/core/spi/OntologyValidator.java
onto-evolve-core/src/main/java/com/ontoevolve/core/model/Assignment.java
onto-evolve-core/src/main/java/com/ontoevolve/core/config/OntoEvolveConfig.java
onto-evolve-plugins/src/main/java/com/ontoevolve/plugins/variator/LLMGenerateVariator.java
onto-evolve-plugins/src/main/java/com/ontoevolve/plugins/variator/CrossoverVariator.java
onto-evolve-infra/src/main/java/com/ontoevolve/infra/validation/JenaOntologyValidator.java
onto-evolve-infra/src/main/java/com/ontoevolve/infra/metrics/MetricsCollector.java
onto-evolve-starter/src/main/java/com/ontoevolve/starter/OntoEvolveAutoConfiguration.java
onto-domain-education/.../service/InterventionService.java
onto-domain-education/.../controller/EventController.java
```

### 保持不变（V2 完整保留）

```
onto-evolve-core/.../kernel/EvolutionEngine.java           ← 不变
onto-evolve-core/.../kernel/DecisionPopulation.java        ← 不变
onto-evolve-core/.../kernel/EvolTrace.java                 ← 不变
onto-evolve-core/.../spi/Variator.java                     ← 不变
onto-evolve-core/.../spi/Selector.java                     ← 不变
onto-evolve-core/.../spi/Migrator.java                     ← 不变
onto-evolve-core/.../spi/Classifier.java                   ← 不变
onto-evolve-graph-store/...                                 ← 不变
```

---

## 七、风险与缓解

| 风险 | 概率 | 影响 | 缓解 |
|------|------|------|------|
| Tool Registry 安全漏洞 | 低 | 高 | SandboxedToolExecutor 多层防护 |
| ReasoningTrace 存储膨胀 | 中 | 中 | 采样 + TTL + 归档，P1-2 实现 |
| Variator 变更破坏 V2 行为 | 低 | 高 | `toolRegistry=null` 全链路兼容性测试 |
| Scenario 时间悖论理解偏差 | 中 | 中 | 所有比较使用 delta，文档明确 baselineGeneration |
| LLM 重试成本不可控 | 中 | 中 | max-attempts=2，metrics 监控告警 |
