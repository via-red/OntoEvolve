# OntoEvolve V3 核心设计 — 从进化到可推理的智能决策

> **版本**: 0.3.0 (规划)
> **最后更新**: 2026-05-15
> **设计基础**: Palantir Ontology 架构启发 + 已有进化内核的天然延伸

---

## ACT 5 — v2 的成就与新地平线

v2 让 OntoEvolve 从一个简单的 "本体+LLM+Bandit" 决策引擎，进化为一个具有完整生态位的进化系统。种群竞争、Pareto 多目标优化、谱系追踪、语义迁移——这些能力让系统不再是"被动响应"，而是"主动进化"。

但当我们审视 Palantir Ontology 这样的企业级系统时，v2 暴露了一个根本性空白：

```
v2 能回答：       "在当前种群中，哪个方案 Pareto 最优？"
v2 不能回答：     "为什么是这个方案？它经历了怎样的推理链？"
v2 不能回答：     "如果当时选择了另一个方案，结果会怎样？"
v2 不能回答：     "人类为什么改写了我的推荐？我从中学到了什么？"
v2 不能做：        "在生成新方案时，调用一个预测模型来计算预期效果"
```

v2 的进化引擎是强大的，但它像一个**黑箱育种实验室**——方案进来了、变异了、被选中了，但整个推理过程对终端用户来说仍然是不透明的。用户看到的只是输入→输出，而中间的**推理链**、**对比分析**、**假设推演**、**人类反馈的深层学习**是缺失的。

V3 的目标不是堆砌新功能，而是回答一个核心问题：

> **如何让进化决策系统从"有效"走向"可信"？**

借鉴 Palantir 的三个洞察——**逻辑工具（OAG）**、**场景推演（Scenario）**、**全链路可解释（CoT）**——V3 在 v2 的进化内核之上，构建了五个相互支撑的扩展层：

```
V3 五大扩展

┌─────────────────────────────────────────────────────────────────────┐
│  ① 逻辑工具注册表 (Tool Registry)                                    │
│     └─ LLM 变异时调用确定性工具 (预测/优化/规则引擎)                  │
│                                                                     │
│  ② 场景推演沙盒 (Scenario Sandbox)                                  │
│     └─ 写时复制的安全假设空间，回答"如果…会怎样"                       │
│                                                                     │
│  ③ 推理路径全链路可视化 (Reasoning Trace)                            │
│     └─ 从事件到决策的每一步：为什么选这个？为什么放弃其他的？             │
│                                                                     │
│  ④ 人类修订捕获闭环 (Human Override Learning)                        │
│     └─ 人类改写 AI 推荐 → 差异分析 → 新个体加入种群                    │
│                                                                     │
│  ⑤ 规则引导变异 (Rule-Guided Variation)                             │
│     └─ 变异前先查规则：不通过→引导重新生成，而非简单否决               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 二、V3 核心更新总览

| # | 特性 | v2 状态 | V3 目标 | 复杂度 | 差异化价值 |
|---|------|---------|---------|--------|-----------|
| 1 | **逻辑工具注册表** | LLM 纯文本生成，无确定性工具调用 | LLM 可调用注册工具（预测/优化/规则） | 中 | ⭐⭐⭐⭐⭐ |
| 2 | **场景推演沙盒** | 无分支假设空间，SimulatedEnvironment 仅为噪声模拟 | Copy-on-Write 分支 + 回放对比 + 合并/丢弃 | 中 | ⭐⭐⭐⭐ |
| 3 | **推理路径可视化** | EvolTrace 记录变异谱系，但无端到端推理链 | 事件→分类→匹配→执行→反馈，每步"为什么" | 低 | ⭐⭐⭐⭐ |
| 4 | **人类修订捕获** | Feedback 只记录数值评分，不捕获人类改写 | Human Override → diff 分析 → 注册新个体 | 低 | ⭐⭐⭐ |
| 5 | **规则引导变异** | OntologyValidator 仅做通过/不通过否决 | 不通过时注入规则约束让 LLM 重新生成 | 中 | ⭐⭐⭐ |

---

## 三、特性一：逻辑工具注册表（Tool Registry）

### 3.1 当前困境

v2 的 Variator（LLMGenerateVariator / CrossoverVariator）在生成新决策方案时，完全依赖 LLM 的文本生成能力。LLM 被要求"写一个干预方案"，它只能基于训练数据中的模式来编造。

这意味着：
- LLM 无法精确计算（如"这个方案的预期效果是多少？"）
- LLM 无法调用外部模型（如"用 Prophet 预测一下这个干预的时间序列效果"）
- LLM 无法执行约束求解（如"在成本低于 100 元的约束下，最优方案是什么？"）
- 生成的方案缺乏数据支撑，仅仅是"看起来合理"

### 3.2 V3 方案：Tool Registry

在 Variator 中引入一个 **Tool Registry**（工具注册表），让 LLM 在生成变异时不仅能写文本，还能调用注册好的确定性工具。

#### 架构

```
┌──────────────────────────────────────────────────┐
│                  Variator                         │
│                                                    │
│  LLM 生成 prompt:                                  │
│    "当前生态位的成功特征: [摘要]                     │
│     可用工具:                                       │
│       1. predict_effect(intervention_params)       │
│       2. optimize_schedule(constraints)            │
│       3. calculate_risk(params)                    │
│     请先生成工具调用，再基于结果生成决策方案"            │
│                                                    │
│  LLM 响应:                                         │
│    Tool Call: predict_effect({type: "talk", ...})  │
│    → Result: 效果 0.78, 成本 0.35                  │
│    → 生成决策: "基于预测效果，建议采用谈话干预..."      │
│                                                    │
│  ToolExecuter:                                      │
│    ├─ 执行工具调用                                   │
│    ├─ 将结果注入 LLM 上下文                           │
│    └─ 收集工具执行统计（延迟、成功率）                  │
└──────────────────────────────────────────────────┘
```

#### SPI 设计

```java
public interface Tool {
    /** 工具名称，LLM 通过此名称调用 */
    String name();
    
    /** 工具描述，注入 LLM prompt 帮助它理解何时调用 */
    String description();
    
    /** 参数 JSON Schema，约束 LLM 生成的参数格式 */
    String parameterSchema();
    
    /** 执行工具逻辑 */
    ToolResult execute(JsonNode parameters, ToolContext ctx);
}

public class ToolResult {
    private final boolean success;
    private final JsonNode data;        // 结构化结果
    private final String summary;       // 注入 LLM 的自然语言摘要
    private final long elapsedMs;
}

public class ToolRegistry {
    void register(Tool tool);
    List<Tool> getAvailableTools();
    ToolResult execute(String toolName, JsonNode params);
}
```

#### 内置工具（开箱即用）

| 工具 | 功能 | 技术实现 | 领域依赖 |
|------|------|---------|---------|
| `predict_effect` | 基于历史反馈预测新方案的预期效果 | 加权 KNN / 轻量级线性回归 | **通用**（有历史反馈即可） |
| `optimize_schedule` | 在成本/时间约束下生成最优调度 | 简单约束求解器 | **通用** |
| `calculate_risk` | 计算方案的风险评分 | 基于本体推理的规则引擎 | **通用** |
| `similar_cases` | 查询历史上相似的场景和采用方案 | Neo4j 图查询 | **通用** |
| `ontology_reason` | 查询 OWL 推理结果（传递性、不相交性） | Apache Jena Inference | **通用** |

> **跨领域说明**：`predict_effect` 的默认实现（加权 KNN）在历史数据充足的领域（教育、客服）效果好，但在**医疗等小样本或隐私受限领域**可能需要替换为其他实现。这是 SPI 设计的天然优势——领域项目可提供自己的 `Tool` 实现覆盖默认行为。所有内置工具均标注领域依赖等级，帮助领域适配者快速决策。

**Tool SPI 本身就是跨领域扩展点**——与 Variator、Selector 等核心 SPI 一样，领域项目可以通过 `@ConditionalOnMissingBean` 注册领域特定的工具，或覆盖内置工具的默认实现。

#### 应用场景示例

```
场景：LLMGenerateVariator 为 "课堂扰乱" 生态位生成新方案

传统 v2 方式：
  LLM 收到 prompt → 写一段 "建议进行课后谈话" → 返回文本
  → 缺点：没有数据支撑，和已有方案没有区别

V3 Tool Registry 方式：
  LLM 收到 prompt → 思考：应该先查一下类似案例 →
  Call tool: similar_cases({concept: "课堂扰乱", top: 3})
    → 返回 3 个历史成功方案
  Call tool: predict_effect({type: "谈话", student_profile: ...})
    → 返回 预期效果 0.82, 置信度 0.76
  → 基于上述数据生成方案："根据历史数据，谈话干预对这类行为的
    预期效果为 0.82，建议在课后谈话基础上增加书面反思环节..."
  → 决策方案附带数据来源，可审计、可追溯
```

### 3.3 安全设计：工具沙盒（Tool Sandbox）

Tool Registry 让 LLM 可以调用注册工具，这带来了**三个安全风险**，必须在设计层面解决：

| 风险 | 场景 | 缓解措施 |
|------|------|---------|
| **参数注入** | LLM 生成恶意参数（`predict_effect({sql: "DROP TABLE"})`） | JSON Schema 严格校验 + 参数类型白名单 |
| **工具崩溃** | 第三方工具抛出未捕获异常，击穿 Variator 线程 | 每个工具在独立 `SandboxedToolExecutor` 中执行，超时 + 隔离 |
| **结果投毒** | 工具返回虚假数据，LLM 基于假数据生成决策写入种群 | 结果 sanitization + 置信度阈值校验 |

#### 安全执行架构

```
LLM 请求调用工具
       │
       ▼
SandboxedToolExecutor:
  ├─ 1. 参数校验: JSON Schema 验证
  │    └─ 不通过 → 返回参数错误，不计入工具调用统计
  ├─ 2. 执行隔离: 
  │    ├─ 独立线程 (CompletableFuture)
  │    ├─ 超时控制 (可配置，默认 5s)
  │    └─ 异常捕获 (任何 Throwable 不传播到主流程)
  ├─ 3. 结果 sanitization:
  │    ├─ 结果大小限制（默认 10KB，防止注入超大 payload）
  │    ├─ 数值范围检查（NaN/Infinity 过滤）
  │    └─ 敏感字段脱敏（可配置的 field filter）
  └─ 4. 统计上报:
       ├─ 成功/失败计数 → MetricsCollector
       ├─ 延迟 P50/P95/P99 → MetricsCollector
       └─ 工具调用频率 → 异常检测（短时间高频调用触发熔断）
```

#### SPI 扩展

```java
public class SandboxedToolExecutor {
    private final long timeoutMs;
    private final long maxResultBytes;
    private final List<FieldFilter> fieldFilters;
    
    public ToolResult executeSafely(Tool tool, JsonNode params) {
        // 1. JSON Schema 校验
        ValidationResult validation = validateParams(tool.parameterSchema(), params);
        if (!validation.passed()) {
            return ToolResult.failure("参数校验失败: " + validation.errors());
        }
        
        // 2. 隔离执行
        CompletableFuture<ToolResult> future = CompletableFuture
            .supplyAsync(() -> tool.execute(params, ctx))
            .orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        
        try {
            ToolResult result = future.get();
            // 3. 结果 sanitization
            return sanitize(result);
        } catch (TimeoutException e) {
            return ToolResult.failure("工具执行超时 (" + timeoutMs + "ms)");
        } catch (Exception e) {
            return ToolResult.failure("工具执行异常: " + e.getMessage());
        }
    }
}
```

#### 安全配置

```yaml
onto:
  tool-registry:
    enabled: true
    sandbox:
      default-timeout-ms: 5000
      max-result-bytes: 10240
      max-calls-per-variation: 5      # 单次变异最多调用 5 次工具
      circuit-breaker:
        error-threshold: 10            # 10 次连续错误后熔断
        reset-timeout-ms: 30000        # 30 秒后自动恢复
```

### 3.4 与已有架构的集成

- `VariationContext` 增加 `ToolRegistry` 字段
- `LLMGenerateVariator` 和 `CrossoverVariator` 在 prompt 中注入可用工具列表
- 现有 Variator 逻辑不变，Tool Registry 作为**可选增强层**（`@ConditionalOnBean`）
- 工具执行结果写入 `EvolTrace`，形成完整的推理证据链

---

## 四、特性二：场景推演沙盒（Scenario Sandbox）

### 4.1 当前困境

v2 的决策是**一次性**的：事件进入 → 分类 → 匹配方案 → 执行 → 收到反馈。

决策者和开发者无法安全地回答：
- "如果当时给这个学生用的是谈话干预而不是警告，效果会更好吗？"
- "在这个生态位引入一个新的变异方案，种群会如何变化？"
- "如果把 Pareto 权重从 [效果优先] 调整为 [成本优先]，推荐方案会变吗？"

v2 的 `SimulatedEvaluationEnvironment` 虽然能模拟反馈，但它是在**主数据流**上操作的，没有"分支→实验→对比→合并/丢弃"的能力。

### 4.2 V3 方案：Scenario Sandbox

在 `Environment` SPI 的基础上，扩展一个 **Scenarios 模块**，提供"写时复制"的沙盒推演能力。

#### 架构

```
┌───────────────────────────────────────────────────────────┐
│                    Scenario Sandbox                        │
│                                                           │
│  createBranch(name: String): ScenarioContext              │
│    → 基于当前种群状态的快照创建分支                         │
│    → 分支拥有独立的 PopulationStore 实例                   │
│                                                           │
│  runScenario(ctx, action): ScenarioResult                 │
│    → 在分支上执行推演（修改方案、调整权重、注入事件）        │
│    → 记录执行结果和种群变化                                │
│                                                           │
│  compareResults(branchA, branchB): ComparisonReport       │
│    → 对比两个分支的最终种群状态、Pareto 前沿、变化轨迹      │
│                                                           │
│  mergeBranch(source, target): MergeResult                  │
│    → 将分支的优秀变异合并回主线种群                        │
│    → 冲突处理：Pareto 支配判定                             │
│                                                           │
│  discardBranch(name)                                      │
│    → 丢弃分支，不影响主线数据                              │
└───────────────────────────────────────────────────────────┘
```

#### 推演工作流

场景推演面临一个根本性挑战：**主线在持续进化，而分支是基于历史快照的**。直接对比主线和分支的绝对值会产生误导。

**解决方案：使用 delta（差异量）而非绝对值，始终标注推演的基准代际。**

```
主线 (Mainline)
  Concept: "课堂扰乱" | Population: [A, B, C, D] | Generation: 12
       │
       │ createBranch("试一下新方案E") 
       │ ← 记录基准: 主线 Gen 12, hypervolume=0.72
       ▼
分支 (Branch: 试一下新方案E)
  Concept: "课堂扰乱" | Population: [A, B, C, D] | Generation: 12  ← 写时复制快照
       │
       │ runScenario: 注入新方案 E, 注入 20 条模拟事件
       │              EvolutionEngine 运行 3 代
       ▼
分支 (Branch: 试一下新方案E)
  Concept: "课堂扰乱" | Population: [A, B, C, D, E] | Generation: 15
       │
       │ 此时主线已进化到 Gen 16，直接对比 Gen 15 vs Gen 16 不公平
       │ 使用 delta 比较：
       ▼
ComparisonReport:
  ├─ baselineGeneration: 12 (分支创建时的主线代际)
  ├─ baselineHypervolume: 0.72
  │
  ├─ 分支: 3 代推演 → hypervolume 从 0.72 到 0.81 = ▲0.09 (+12.5%)
  ├─ 主线: 同期 3 代 (Gen 12→15) → hypervolume 从 0.72 到 0.75 = ▲0.03 (+4.2%)
  │
  ├─ 增量对比: 分支的 hypervolume 增长是主线的 3.0x
  ├─ 新方案 E 在 20 次模拟中进入前沿 5 次
  └─ 分支推荐：E 在效果维度显著优于 C，建议合并
       │
       │ mergeBranch(分支, 主线)
       ▼
主线 (合并后)
  Concept: "课堂扰乱" | Population: [A, B, E, D] | Generation: 17
  (C 被 E 在 Pareto 意义下淘汰)
```

**关键原则：**
1. `ComparisonReport` 必须包含 `baselineGeneration` 字段
2. 所有对比使用 delta（`分支增量 - 主线同期增量`），而非 `分支终值 - 主线终值`
3. Dashboard 上清晰标注分支所基于的代际（"基于 Gen 12，已推演 3 代"）
4. 如果主线在推演期间发生了重大变化（如管理员手动干预），分支标注为"可能已过期"

#### 技术实现

| 组件 | 说明 |
|------|------|
| `ScenarioContext` | 分支上下文，含独立 `PopulationStore` 实例（`InMemoryScenarioStore` 用 `ConcurrentHashMap` 快照） |
| `InMemoryScenarioStore` | 实现 `PopulationStore` 接口的写时复制版本，`clone()` 时复制热缓存 |
| `ScenarioOrchestrator` | 推演编排器：创建分支 → 执行推演 → 生成对比报告 → 合并/丢弃 |
| `ComparisonReport` | 结构化对比结果：hypervolume 差异、Pareto 前沿变化、每个方案的统计变化 |
| `MergeStrategy` | 合并策略接口：`PARETO_DOMINANCE` / `ELITE_PRESERVE` / `MANUAL_SELECT` |

#### REST API

```
POST   /api/scenario/branch              — 创建推演分支
POST   /api/scenario/branch/{name}/run   — 在分支上执行推演
GET    /api/scenario/compare?b1=X&b2=Y   — 对比两个分支
POST   /api/scenario/merge?from=X&to=Y   — 合并分支
DELETE /api/scenario/branch/{name}       — 丢弃分支
GET    /api/scenario/branches            — 列出所有分支
```

### 4.3 与已有架构的集成

- `Environment` SPI 新增 `ScenarioSupport` 可选接口，`SimulatedEvaluationEnvironment` 实现
- `InMemoryPopulationStore` 增加 clone 语义（写时复制）
- 场景分支的 EvolTrace 带 `scenario` 标签，与主线轨迹隔离
- Dashboard 前端新增 "场景推演" 页面：分支管理 + 对比可视化

---

## 五、特性三：推理路径全链路可视化（Reasoning Trace）

### 5.1 当前困境

v2 的 `EvolTrace` 是一个优秀的谱系追踪系统，但它记录的是**变异事件的元数据**，而不是**单次决策的完整推理过程**。

用户（教师、管理员）看到的是：
- "推荐方案：课后谈话" ← 只知道结果，不知道为什么

系统内部有丰富的信息但未暴露：
- 分类器为何将这个事件归类为"课堂扰乱"而非"社交冲突"？
- Matcher 为何在 4 个候选方案中选择了这个（Pareto 排名？UCB 探索？）
- 这个方案的历史效果如何（均值、方差、样本量）？
- 如果选了排名第二的方案，预期效果差多少？

### 5.2 V3 方案：Reasoning Trace

在 `EvolTrace` 的基础上，构建一个**端到端的推理链追踪系统**，记录从事件到决策每一步的"为什么"。

#### 推理链数据结构

```java
public class ReasoningTrace {
    private String traceId;                    // 唯一追踪 ID
    private String eventId;                    // 原始事件 ID
    private List<ReasoningStep> steps;         // 推理步骤链
    
    public record ReasoningStep(
        String stepName,                       // "classification" | "matching" | "execution"
        String description,                    // 人类可读的描述
        Map<String, Object> inputs,            // 步骤输入
        Map<String, Object> outputs,           // 步骤输出
        Map<String, Object> rationale,         // 决策依据（关键！）
        long elapsedMs,                        // 耗时
        List<String> alternatives              // 被放弃的替代方案及原因
    ) {}
}
```

#### 推理步骤详细设计

| 步骤 | 记录内容 | "为什么" 信息 |
|------|---------|-------------|
| **分类** (Classification) | 输入事件 → 输出 Concept | 分类置信度、各候选类别得分、LLM 推理摘要、是否回退到父类 |
| **匹配** (Matching) | Concept → 选中的 Assignment | 种群内排名、各方案的 Pareto 得分和 UCB 值、是否为随机探索、是否层级回退 |
| **执行** (Execution) | 选中的方案 → 执行结果 | 执行参数、执行者、执行上下文 |
| **评价** (Feedback) | 执行结果 → 多维评分 | 评分向量、是否超预期/低于预期、历史均值对比 |
| **进化** (可选，当触发 Evolution) | 种群变化 | 变异算子类型及成本、淘汰方案及原因、新方案与已有方案的 Pareto 对比 |

#### 分层展示策略

推理路径的展示必须解决**信息过载**问题——技术用户想看到全部数据，业务用户只需要 3-5 个关键信息。设计上采用**三层渐进披露**：

```
层级 1: 结论摘要 (默认可见)
─────────────────────────────────────────────────
  ✅ 事件已处理 | 分类: 课堂扰乱 | 方案: 课后谈话
  耗时: 1.2s | 置信度: 高 (0.92)
  ─── 点击展开查看详情 ───

层级 2: 推理链概览 (点击展开)
─────────────────────────────────────────────────
  ① 分类 → 课堂扰乱 (0.92)
     次选: 社交冲突 (0.05) → 排除原因: ...
  ② 匹配 → 课后谈话 (#42, Gen 7)
     第 2 名: 书面警告 (差异: +0.03 效果 / +0.21 成本)
  ③ 执行 → 已完成
  ④ 反馈 → 已接收 (3 维评分)
  ─── 查看完整技术数据 ───

层级 3: 原始数据 (技术用户 / API)
─────────────────────────────────────────────────
  分类 LLM 完整响应: { ... }
  种群快照 (代际 7): [ ... ]
  Pareto 排名计算过程: [ ... ]
  完整的 ReasoningTrace JSON
```

API 设计上同样分层：

```
GET /api/trace/{eventId}?detail=summary   → 层级 1（默认，最快）
GET /api/trace/{eventId}?detail=overview  → 层级 2
GET /api/trace/{eventId}?detail=full      → 层级 3（含完整推理链数据）
```

#### 前端展示（增强现有 Events 页面）

```
事件详情页 V3 增强
═══════════════════════════════════════════════════════

事件: "张三在课堂上大声说话，打断老师讲课"
分类: 课堂扰乱 (置信度 0.92)
  └─ 次选: 社交冲突 (0.05) — 因 "行为描述未涉及人际冲突" 被排除
  └─ 次选: 注意力缺失 (0.03)
                                                         
推荐方案: 课后谈话 (Assignment #42, Gen 7)              
  ┌─ Pareto 排名: 第 1 前沿 | 拥挤距离: 0.35           
  ├─ 效果: 0.82 (±0.12, n=23)  ← 历史均值+方差+样本量   
  ├─ 成本: 0.31 (±0.08, n=23)                           
  ├─ 满意度: 0.74 (±0.15, n=23)                         
  ├─ [第 2 名方案] 书面警告 (排名: 第 1 前沿, 拥挤距离: 0.28)
  │    — 效果略高 (0.85) 但成本更高 (0.52)，综合未胜出   
  └─ [第 3 名方案] 课后留堂 (排名: 第 2 前沿)
       — 被 A(谈话) 和 B(警告) 同时 Pareto 支配          
                                                         
执行结果: 已执行 (由 王老师)                             
  └─ 反馈: [效果: 0.80, 成本: 0.30, 满意度: 0.85]      
  └─ 与历史均值差异: 在预期范围内 (+0.01 σ)             
```

#### 实现策略

| 组件 | 说明 |
|------|------|
| `ReasoningTraceCollector` | 拦截器/切面，自动采集各环节的推理数据 |
| `ReasoningTraceRepository` | 存储推理链（Neo4j 中的新节点类型，或独立索引） |
| `ReasoningTraceController` | REST API：`GET /api/trace/{eventId}` |
| 前端增强 | Events 页面的展开详情区增加推理链展示 |

#### 存储策略：采样与归档

Reasoning Trace 在每天处理数千事件的场景下，会产生 GB 级 JSON 数据。必须在第一天就设计存储策略，否则上线即故障。

**采样策略**

| 环境 | 采样率 | 完整数据 | 摘要数据 |
|------|--------|---------|---------|
| 开发/调试 | 100% | ✅ 全量存储 | ✅ |
| 生产 (默认) | 10% | ✅ 全量 | ✅ (100% 事件) |
| 生产 (高负载) | 1% | ✅ 全量 | ✅ (100% 事件) |

- **摘要数据**：轻量，每条几百字节，包含事件 ID、分类结果、方案 ID、关键置信度、耗时。100% 事件都存储。
- **完整数据**：含 LLM 完整响应、种群快照、Pareto 计算过程。按采样率存储。
- 采样率运行时动态可调（通过 Actuator 或配置中心），无需重启。

**归档策略**

```
存储分层:
  ┌─ 热存储 (Neo4j / Redis)
  │    保留最近 7 天的 trace
  │    快速查询: < 50ms
  │
  ├─ 温存储 (PostgreSQL JSONB / MongoDB)
  │    保留 7-90 天的 trace
  │    按需查询: < 500ms
  │    夜间定时迁移 (Batch job)
  │
  └─ 冷存储 (对象存储 / 压缩文件)
       保留 > 90 天的 trace
       仅支持批量导出，不支持在线查询
       每月自动清理 (可配置 TTL)
```

**配置化**

```yaml
onto:
  reasoning-trace:
    enabled: true
    sampling-rate: 0.1              # 生产采样率 10%
    summary-store: neo4j            # 摘要存储后端
    detail-store: postgresql        # 详细数据存储后端
    detail-ttl-days: 90             # 详细数据保留天数
    archive:
      enabled: true
      cron: "0 3 * * *"            # 每天凌晨 3 点归档
      compression: gzip
```

#### 配置化开关

Reasoning Trace 整体设计为**可选特性**，通过 `onto.reasoning-trace.enabled` 控制：

- 开发环境：开启（100% 采样）
- 生产环境：开启（按采样率）
- 极端高负载：关闭（零开销）

关闭时，`ReasoningTraceCollector` 不创建 bean，`InterventionService` 中通过 `@Autowired(required=false)` 或 `Optional` 处理。零运行时开销。

### 5.3 与已有架构的集成

- `InterventionService` 的方法调用处插入 `ReasoningTraceCollector` 调用
- `EvolTrace` 保持不变，`ReasoningTrace` 是补充而非替代
- Neo4j 新增 `ReasoningTraceNode` 节点类型（或使用 JSON 存储在现有节点上）
- Dashboard 前端展示推理链，与现有方案库 / 谱系树联动

---

## 六、特性四：人类修订捕获闭环（Human Override Learning）

### 6.1 当前困境

当 AI 推荐一个方案，而人类（教师、管理员）选择**修改**它时，v2 只能收到一个数值反馈（"效果 0.7"），完全丢失了最宝贵的信息——**人类改了什么，以及为什么改**。

```
AI 推荐: "课后谈话 + 书面反思"
人类实际执行: "课后谈话 + 家长沟通"
                                ↑
                        这个差异是金矿！
但 v2 只记录了: Feedback(effectiveness=0.7)
```

人类对 AI 推荐的每一次改写，都是**领域知识的一次显式表达**。丢失这个信号，等于让系统在黑暗中摸索。

### 6.2 V3 方案：Human Override Learning

在 `Assignment` 生命周期和 `Feedback` 机制中增加人类修订捕获能力。

#### 工作流

```
AI 推荐方案 A
       │
       ▼
人类审阅 ──→ 接受 → 执行 → 正常反馈循环
       │
       └──→ 修改为方案 B
                │
                ▼
        Human Override Captured:
          ├─ 原始方案 A (AI 推荐)
          ├─ 实际方案 B (人类执行)
          ├─ diff(A, B):
          │   ┌────────────┬──────────┬──────────┐
          │   │ 维度        │ A        │ B        │
          │   ├────────────┼──────────┼──────────┤
          │   │ 类型        │ 谈话     │ 谈话     │
          │   │ 执行人      │ 班主任   │ 班主任   │
          │   │ 附加措施    │ 书面反思 │ 家长沟通 │  ← 差异点
          │   │ 时长(分)    │ 15       │ 25       │  ← 差异点
          │   └────────────┴──────────┴──────────┘
          ├─ 人类备注: "该生已多次书面反思无效，需家长配合"
          └─ 系统行动:
               ├─ B 作为新 Assignment 注册到当前种群
               ├─ B 继承 A 的历史评分作为初始值
               ├─ B 的 parent 指向 A (谱系)
               └─ A 被标记为 "被人类否决" 状态
```

#### SPI 扩展

```java
public interface HumanOverrideHandler<A extends Assignment> {
    /** 分析人类改写差异，返回分析报告 */
    OverrideAnalysis analyzeOverride(A original, A actual, String note);
    
    /** 决定如何处理改写后的方案 */
    OverrideAction decideAction(OverrideAnalysis analysis);
}

public record OverrideAnalysis(
    A original,                     // AI 推荐的原始 Assignment
    A actual,                       // 人类实际执行的方案
    Map<String, DiffEntry> diffs,   // 逐维度差异
    String note,                    // 人类备注
    double dissimilarity            // 差异度 [0, 1]
) {}

public enum OverrideAction {
    REGISTER_AS_NEW,      // 注册为新个体（差异度大）
    UPDATE_EXISTING,      // 更新现有方案（差异度小）
    IGNORE               // 忽略（差异度极小，如措辞微调）
}
```

#### REST API 扩展

```
现有:   POST /api/education/evaluation  — 提交评分
新增:   POST /api/education/override     — 提交人类修订
          Body: { eventId, originalAssignmentId, 
                  modifiedDecision: { name, steps, ... }, 
                  note: "修改原因" }
```

#### 前端增强

Events 页面中的执行表单增加"修改后执行"选项：

```
当前执行表单:
  ┌────────────────────────────────────────────┐
  │ 推荐方案: 课后谈话                           │
  │                                            │
  │ ○ 按推荐方案执行                             │
  │ ● 修改后执行  ← 新选项                       │
  │                                            │
  │ 修改内容:                                    │
  │ ┌──────────────────────────────────────────┐│
  │ │ 执行人: 班主任          [✓]              ││
  │ │ 附加措施: [家长沟通]    [✓]  ← 预填推荐值││
  │ │ 时长(分): 25                              ││
  │ │ ...                                      ││
  │ └──────────────────────────────────────────┘│
  │ 修改原因: 该生已多次书面反思无效              │
  └────────────────────────────────────────────┘
```

### 6.3 与已有架构的集成

- `Assignment` 新增状态 `HUMAN_OVERRIDE` 和 `SUPERSEDED`
- `Assignment` 新增 `humanOverrideNote` 和 `supersededBy` 字段
- `HumanOverrideHandler` 默认实现自动注册新个体
- 新个体继承父代的评分向量作为先验（Bayesian warm-start）
- 前端谱系图区分"AI 生成"和"人类改写"节点（不同颜色/图标）

---

## 七、特性五：规则引导变异（Rule-Guided Variation）

### 7.1 当前困境

v2 的 `JenaOntologyValidator` 是一个**否决闸门**——变异生成的方案如果违反 OWL 约束（类不存在、不相交类冲突、模型不一致），直接拒绝。

问题在于：否决没有学习价值。

```
LLM 生成了方案: "给课堂扰乱的学生开处方药" (违反医学领域约束)
OntologyValidator: ❌ 拒绝
LLM 下次生成: "给课堂扰乱的学生开处方药" (又来了，因为 LLM 不知道上次为什么被拒)
```

**否决是零反馈的**——系统只说不，没说不的原因，也没说怎么改。

### 7.2 V3 方案：Rule-Guided Variation

将 `OntologyValidator` 从"否决闸门"升级为"引导教练"。当变异违反规则时，提取规则约束注入 LLM 的重新生成 prompt，让下一次生成在规则边界内进行。

#### 工作流

```
LLM 生成方案 D
       │
       ▼
Rule-Guided Validator:
  ├─ 检查 1: OWL 类存在性 → ✅ 通过
  ├─ 检查 2: disjointness → ❌ 违反
  │   └─ 详细: "方案 D 的 intervention_type='reward' 与 
  │             concept='Cheating' 存在 disjointness 约束"
  ├─ 检查 3: 一致性 → ✅ 通过
  │
  └─ 结果: ❌ 不通过
       │
       ▼
   生成修正反馈:
      "你生成的方案被以下规则拒绝:
       Rule: Cheating ⊑ ¬usesReward
       约束: 作弊类行为不得使用奖励类干预
       建议: 将 intervention_type 改为 'talk' 或 'notice'"
       │
       ▼
   LLM 重新生成 D'
       │
       ▼
   Rule-Guided Validator → ✅ 通过 → 进入种群
```

#### Validator SPI 扩展

```java
public interface OntologyValidator {
    /** V3 替换 V2 的 validate()：获取详细验证报告，包含违反的规则和修正建议 */
    ValidationReport validateWithReport(Assignment assignment, ValidationContext ctx);
}

public class ValidationReport {
    private final boolean passed;
    private final List<RuleViolation> violations;
    private final String correctionPrompt;  // 注入 LLM 的修正引导
    private final int severity;             // 1=建议 2=警告 3=强制
    
    public record RuleViolation(
        String ruleName,            // 规则名称（如 "CheatingNoReward"）
        String ruleDescription,     // 人类可读描述
        String owlAxiom,            // OWL 公理原文
        String[] involvedEntities,  // 涉及的实体
        String suggestion           // 修正建议
    ) {}
}
```

#### 规则来源

| 来源 | 示例 | 优先级 |
|------|------|--------|
| OWL TBox 推理 | `disjointClasses(:Academic :Behavioral)` | 强制 |
| OWL 属性约束 | `hasSeverity range xsd:integer[0,10]` | 强制 |
| SWRL 规则 | `Cheating(?c) ∧ usesIntervention(?c, ?i) ∧ Reward(?i) → Invalid` | 强制 |
| 领域配置规则 | `成本不能超过 100 元` | 警告 |
| 历史经验规则 | `过去 30 天该方案效果低于 0.3` | 建议 |

#### 配置化重试策略

每次规则违反后的 LLM 重试都是一次成本支出。必须严格约束重试行为：

```yaml
onto:
  variation:
    rule-guided-retry:
      max-attempts: 2                  # 最多重试 2 次（不是 3 次）
      temperature-decay: 0.2           # 每次重试降低 0.2 创造力参数
      backoff-ms: 500                  # 重试间隔
      fallback-behavior: BYPASS        # 超上限后：BYPASS(绕过) / DISCARD(丢弃) / FALLBACK_TO_PERTURB
    metrics:
      violation-rate-alert-threshold: 0.3   # 违反率 > 30% 告警
```

**告警触发条件：** 当 `规则违反率 > 30%` 或 `retry 耗尽率 > 5%` 时，系统自动发出告警。高违反率通常意味着：
- 本体约束定义有误（过于严格或存在矛盾）
- 新领域冷启动，LLM 尚未学会领域边界
- Variator prompt 模板需要调整

#### 与已有架构的集成

- `JenaOntologyValidator` 增加 `validateWithReport()` 实现
- `LLMGenerateVariator` 增加重试循环：生成 → 验证 → (不通过) → 注入修正 prompt → 重新生成
- 重试上限可配置（默认 2 次，详见配置化重试策略），超过上限则回退到 LLM 直接生成（绕过规则）
- 每次规则违反记录到 `MetricsCollector`（规则违反率、按规则分类统计）
- 规则违反数据在 Dashboard 中展示，帮助领域专家发现常见违规模式

---

## 八、跨领域泛化注意事项

V3 的设计文档以教育领域为用例展开，但 OntoEvolve 是框架级产品，所有特性必须在 SPI 层面预留跨领域扩展点。以下是每个特性在不同领域下需要关注的适配点。

### 8.1 Tool Registry：predict_effect 的领域敏感度

| 领域 | 数据特征 | 默认 KNN 是否适用 | 建议的替代/增强方案 |
|------|---------|-----------------|-------------------|
| 教育 | 反馈数据充足，维度明确 | ✅ 适用 | — |
| 客服工单 | 反馈数据充足，但噪声大 | ✅ 适用 | 增加异常值过滤 |
| 医疗分诊 | 样本量小，隐私受限 | ❌ 不适用 | 改用迁移学习或少样本模型，数据不出域 |
| 金融风控 | 样本不平衡（正常>>异常） | ⚠️ 需调整 | 加权损失函数或异常检测专用工具 |

**框架层面**：`predict_effect` 的默认实现是可替换的 Tool Bean，领域项目通过 `@ConditionalOnMissingBean` 覆盖。这与 V2 的 SPI 哲学一致。

### 8.2 Scenario Sandbox：种群规模的分层策略

SaaS 多租户场景下，不同租户的种群规模差异可能巨大（100 到 100000），单一的快照策略不可行：

| 租户类型 | 种群规模 | 快照策略 | 内存开销 |
|---------|---------|---------|---------|
| 小租户 | < 1,000 | 全量内存快照（写时复制） | KB 级 |
| 中租户 | 1,000 - 50,000 | 仅快照 Pareto 前沿 + 统计摘要 | MB 级 |
| 大租户 | > 50,000 | 仅快照统计摘要（均值/方差/前沿 hypervolume） | KB 级 |

**框架层面**：`ScenarioContext` 的 `snapshotStrategy` 为可配置策略接口，不同租户级别选择不同实现。默认实现根据种群规模自动选择。

### 8.3 Human Override：结构化 vs 非结构化 diff

Human Override 的 diff 分析在不同领域面临的核心差异：

```
教育领域 (结构化)             医疗领域 (半结构化)          客服领域 (非结构化)
────────────────────          ────────────────────        ────────────────────
intervention_type: str        diagnosis: enum            response_text: free text
steps: List<String>           procedures: List<Code>     attachments: List<URI>
duration: int                 dosage: Decimal            priority: enum

diff = 字段级别比较           diff = 编码匹配 +          diff = 语义相似度
       明确、简单                   参数差异计算                  (embedding cosine)
```

**框架层面**：`HumanOverrideHandler` 的 `analyzeOverride()` 方法接收一个 `DiffStrategy` 参数，领域项目可注入结构化 diff、语义 diff 或混合策略。默认实现提供通用的字段级比较。

### 8.4 Ontology Validator：规则来源的领域差异

| 领域 | 规则来源 | 复杂度 | 规则引导效果 |
|------|---------|--------|------------|
| 教育 | OWL disjointness + 少量配置 | 低 | 高（规则明确） |
| 医疗 | OWL + SNOMED CT/ICD 编码约束 | 高 | 中（编码体系庞大） |
| 金融 | OWL + 合规法规文本（非形式化） | 高 | 低（需 NLP 提取规则） |

**框架层面**：`ValidationReport.correctionPrompt` 的生成由 `RuleInjectionPromptBuilder` SPI 负责，领域项目可覆盖，将领域特定的规则表述方式注入 LLM prompt。

### 8.5 领域适配清单总表

| V3 特性 | 通用实现 | 领域可覆盖点 | SPI 接口 |
|---------|---------|------------|---------|
| Tool Registry | 内置 5 工具（KNN/约束求解/规则/图查询/Jena） | 任意工具实现 | `Tool` |
| Tool Sandbox | JSON Schema + 超时 + 结果校验 | 参数校验器、字段过滤器 | `ParamValidator`, `FieldFilter` |
| Scenario Sandbox | 全量内存快照 | 快照策略、模拟评估逻辑 | `SnapshotStrategy`, `Environment` |
| Reasoning Trace | 三层披露 + 采样归档 | 步骤采集器、存储后端 | `TraceCollector`, `TraceStore` |
| Human Override | 字段级 diff + 自动注册 | diff 策略、决策逻辑 | `DiffStrategy`, `OverrideAction` |
| Rule-Guided Var. | OWL 规则 + Jena 推理 | 规则来源、修正 prompt 模板 | `RuleProvider`, `CorrectionPromptBuilder` |

---

## 九、V3 技术架构全景

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         V3 新增层                                          │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────────┐│
│  │ ① Tool Registry                               (onto-evolve-plugins)  ││
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌───────────────────┐ ││
│  │  │predict_eff │ │optimize    │ │calculate   │ │ similar_cases     │ ││
│  │  │ ect        │ │_schedule   │ │_risk       │ │ (Neo4j query)     │ ││
│  │  └────────────┘ └────────────┘ └────────────┘ └───────────────────┘ ││
│  └──────────────────────────────────────────────────────────────────────┘│
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────────┐│
│  │ ② Scenario Sandbox                          (onto-evolve-core 扩展)  ││
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌─────────────┐ ││
│  │  │Scenario     │ │InMemory     │ │Scenario     │ │MergeStrategy│ ││
│  │  │Orchestrator │ │ScenarioStore │ │Orchestrator  │ │             │ ││
│  │  └──────────────┘ └──────────────┘ └──────────────┘ └─────────────┘ ││
│  └──────────────────────────────────────────────────────────────────────┘│
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────────┐│
│  │ ③ Reasoning Trace                            (onto-evolve-core 新增) ││
│  │  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────────┐ ││
│  │  │ReasoningTrace    │ │ReasoningTrace    │ │ReasoningTrace       │ ││
│  │  │Collector         │ │Repository        │ │Controller           │ ││
│  │  └──────────────────┘ └──────────────────┘ └──────────────────────┘ ││
│  └──────────────────────────────────────────────────────────────────────┘│
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────────┐│
│  │ ④ Human Override + ⑤ Rule-Guided Validator  (现有模块扩展)           ││
│  │  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────────┐ ││
│  │  │HumanOverride     │ │JenaOntology      │ │RuleInjection        │ ││
│  │  │Handler           │ │Validator (增强)   │ │PromptBuilder        │ ││
│  │  └──────────────────┘ └──────────────────┘ └──────────────────────┘ ││
│  └──────────────────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                         V2 现有层 (不变)                                   │
│                                                                          │
│  onto-evolve-core        onto-evolve-plugins      onto-evolve-infra       │
│  ┌──────────────────┐  ┌────────────────────┐  ┌──────────────────────┐  │
│  │ EvolutionEngine  │  │ ParetoCrowding     │  │ LLMClient            │  │
│  │ DecisionPopulation│  │ Selector           │  │ JenaOntologyValidator│  │
│  │ Variator/Selector │  │ LLMGenerateVar     │  │ MetricsCollector     │  │
│  │ Migrator          │  │ CrossoverVar       │  │ PromptTemplateService│  │
│  │ MetaOptimizer     │  │ PerturbVar         │  │ TDB2 Triple Store    │  │
│  │ EvolTrace         │  │ SemanticMigrator   │  │                      │  │
│  │ PopulationStore   │  │ UniformCredit      │  │                      │  │
│  │                   │  │ ParetoUCBMatcher   │  │                      │  │
│  └──────────────────┘  └────────────────────┘  └──────────────────────┘  │
│                                                                          │
│  onto-evolve-graph-store              onto-evolve-starter                │
│  ┌───────────────────────────────┐  ┌──────────────────────────────────┐│
│  │ 7 Neo4j @Node Entities        │  │ AutoConfiguration                ││
│  │ Neo4jPopulationStore          │  │ ConditionalOnBean/Ontology       ││
│  │ ModelMapper                   │  │ YAML Config                      ││
│  └───────────────────────────────┘  └──────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 十、V3 路线图

### Phase 1: 推理可解释性 (1—2 个月)

| 任务 | 涉及模块 | 优先级 |
|------|---------|--------|
| ReasoningTrace 数据模型设计 | onto-evolve-core (新增) | P0 |
| ReasoningTraceCollector 实现 | onto-evolve-core | P0 |
| ReasoningTraceRepository + Controller | onto-evolve-graph-store | P0 |
| 前端推理链展示 (Events 页增强) | frontend | P1 |
| HumanOverrideHandler SPI 设计 | onto-evolve-core | P1 |
| 人类修订捕获前端表单 | frontend | P1 |

### Phase 2: 确定性工具 (2—3 个月)

| 任务 | 涉及模块 | 优先级 |
|------|---------|--------|
| Tool / ToolRegistry SPI 设计 | onto-evolve-core (新增) | P0 |
| SandboxedToolExecutor | onto-evolve-core (新增) | P0 |
| `predict_effect` 工具实现 (KNN) | onto-evolve-plugins | P0 |
| `similar_cases` 工具实现 (Neo4j) | onto-evolve-plugins + graph-store | P0 |
| `calculate_risk` 工具实现 (规则) | onto-evolve-plugins | P1 |
| LLMGenerateVariator Tool Registry 增强 | onto-evolve-plugins | P0 |
| CrossoverVariator Tool Registry 增强 | onto-evolve-plugins | P1 |
| Tool 执行统计 + Metrics | onto-evolve-infra | P1 |
| Reasoning Trace 采样归档策略实现 | onto-evolve-infra | P1 |

### Phase 3: 规则引导与沙盒 (3—5 个月)

| 任务 | 涉及模块 | 优先级 |
|------|---------|--------|
| `validateWithReport()` SPI 扩展 | onto-evolve-core | P0 |
| JenaOntologyValidator 增强 | onto-evolve-infra | P0 |
| RuleInjectionPromptBuilder | onto-evolve-plugins | P0 |
| Variator 重试循环 + 退避策略 | onto-evolve-plugins | P0 |
| ScenarioOrchestrator 设计 | onto-evolve-core (新增) | P0 |
| InMemoryScenarioStore 实现 | onto-evolve-plugins | P0 |
| Scenario REST API | onto-evolve (各层) | P1 |
| 前端场景推演页面 | frontend | P1 |
| 规则违反 Dashboard 卡片 | frontend | P2 |

---

## 十一、V3 的哲学定位

V3 回答的核心问题不是 "系统能不能进化"，而是 **"进化系统能不能被人类信任"**。

```
v1 的核心矛盾:     僵化 vs 幻觉 (本体 vs LLM)
v2 的解决方案:     用本体骨架 + LLM变异 + 帕累托选择
                   让"僵化"与"幻觉"变成正交的两个维度

v2 遗留的问题:     进化过程对终端用户不透明
                   LLM 生成的决策缺乏数据支撑
                   人类的修正经验被浪费
                   否决说"不"但不教"怎么说对"

V3 的核心主题:     可信进化
                   ──────────
                   ① 工具注册 → 决策有数据支撑
                   ② 场景推演 → 决策前可安全实验
                   ③ 推理链路 → 决策过程完全透明
                   ④ 人类捕获 → 每次修正都是学习信号
                   ⑤ 规则引导 → 否决的同时教会"怎么说对"
```

V3 不是对外部系统的模仿，而是在 OntoEvolve 自身的进化内核之上，自然生长出的"成熟层"。v2 让系统能进化，V3 让进化的过程本身可理解、可实验、可协作、可信任。

> **V3 的愿景：进化不再是黑箱，而是一场人类和 AI 共同参与、可以安全推演、每一环都有据可查的协作智能过程。**
