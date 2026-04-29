# OntoEvolve — 本体驱动的自进化决策框架

> **赋予系统在语义约束下自主进化决策方案的能力**

---

## 一、演进历程：从困境到进化

> **为什么我们需要一个真正能进化的框架？**

---

### ACT 1 — 纯本体论的困境

传统专家系统与本体论方法以严谨著称。它们提供清晰的语义、可审计的推理和类型安全。但面对复杂的现实世界，这种严格也是它的致命弱点：

- ✕ 面对未预见的新颖输入极其脆弱
- ✕ 需要领域专家进行昂贵的手工梳理
- ✕ 静态的结构无法从执行结果中学习
- ✕ 无法自主生成新的决策选项

> "本体擅长描述秩序，但拙于面对未知。"

僵化 **Ossification** · 幻觉 **Hallucination**

---

### ACT 2 — 纯大模型的困境

现代的"大语言模型即决策者"方案看似万能。它能处理开放式的自然语言，低成本快速上手，并随时生成新颖的选项。但当它被放入严肃的决策闭环中时：

- ✕ **幻觉**：生成看似合理但在领域语义上无效的决策（如"给学生开处方"）
- ✕ **无记忆**：相同的错误在不同调用中反复出现
- ✕ **无审计轨迹**：无法回答"为什么做出这个推荐"
- ✕ **无累积进化**：即使有反馈，每一次调用依然是从零开始

> "大模型擅长生成可能性，但拙于积累确定性。"

---

### ACT 3 — OntoEvolve v1：第一次合流

为了打破上述困境，我们构建了 v1 架构：**Ontology + LLM + UCB Bandit**。它通过三个扩展点（Classifier, Matcher, Optimizer）将生成能力与优化能力桥接在本体上。

```
Classifier (LLM) ──┐
Matcher (LLM) ─────┤──► Ontology ──► Meta-Concepts
Optimizer (Bandit) ┘
```

> "用本体作为骨架，让 LLM 处理边界模糊的语义，让 Bandit 处理确定性的优化。"

v1 解决了两种纯粹方法的极端缺陷，向系统级智能迈出了关键一步。

---

### ACT 4 — v1 的裂痕：自优化不是自进化

但随着系统的深入运行，我们发现了更深层的问题。一个仅仅依赖 UCB 算法和被动 LLM 生成的框架，虽然能在线学习，但它缺乏生命系统的真正活力。

| # | 问题 | 表现 |
|---|------|------|
| 1 | **变异是被动且盲目的** | 只有当匹配失败时才触发生成，缺乏主动探索机制，无法借鉴历史成功组件 |
| 2 | **选择是单维静态的** | 单一的 UCB 分数掩盖了效果与成本的权衡，也无法适应非平稳的动态环境 |
| 3 | **没有种群与生态位** | 每个方案都是孤立的个体，不交换信息，没有重组，好方案无法跨类别迁移 |
| 4 | **概念膨胀与推理瓶颈** | 缺乏有效的新概念审核机制，庞大的历史记录可能拖垮本体推理性能 |
| 5 | **元层次硬编码** | 演化机制本身（如学习率、生命周期阈值）是静态的，系统不能进化其进化能力 |

> **这些裂痕指向同一件事：v1 是一个能在线学习的决策引擎，但还不是一个真正能进化的复杂自适应系统。**

---

## 二、愿景

### 核心理念

> **本体定义"能成为什么"，进化探索"该成为什么"。**

OntoEvolve 是一个通用的**本体驱动的决策—反馈—进化框架**。它用 OWL 本体作为知识表示层与语义约束层，将 LLM 的生成能力与进化算法的优化能力桥接起来，形成一个可以持续自进化的智能决策系统。

### 设计原则

1. **本体是进化的骨架，不是枷锁** — TBox 提供类型安全的变异空间和硬约束，ABox 承载种群动态变化
2. **LLM 处理边界模糊，进化算法处理确定性优化** — LLM 做分类与变异生成（需语义理解），进化算法做选择与排序（需精确计算）
3. **层次回退保证鲁棒性** — 子概念无匹配时回退至父概念，确保系统总有响应
4. **开方式适应** — 规避单一目标带来的短视效应，在多目标权衡中保留多样性
5. **可解释性是第一性要求** — 谱系树、本体因果链和 Pareto 前沿使每一次决策可审计、可溯源

### 适用场景

- **决策后果严重**：教育干预、医疗分诊、风险控制、政策制定
- **需要长期知识沉淀**：方案效果需跨时间、跨场景累积评估
- **领域规则硬约束**：安全合规要求不能被后置惩罚覆盖
- **需要可解释**：利益相关者需要理解"为什么推荐这个方案，还有哪些备选"

### 不适用的场景

- **毫秒级实时推荐**（电商、广告）：进化循环的异步调度无法匹配
- **冷启动阶段的数据稀疏**：初期种群空洞，依赖 LLM 盲生成
- **纯序列决策**（机器人控制、游戏）：需结合强化学习的外层调度

---

## 三、核心概念

### 元本体层（框架不可变骨架）

| 概念 | 定义 | 领域实例（教育） |
|---|---|---|
| **InputEvent** | 域外输入事件，整个决策循环的起点 | 一条学生行为记录 |
| **Concept** | 本体概念类别，构成生态位地图，支持 parentConcept 层次 | 行为类型"课堂扰乱" |
| **Decision** | 可执行的方案模板，进化的基本单元（个体） | 干预措施"课后谈话" |
| **Assignment** | 将 Decision 绑定到 Concept 的关联实体，携带演化状态 | 某次干预分配（含 score 向量、代际、状态） |
| **Execution** | 方案执行记录，反馈的锚点 | 老师执行了一次"课后谈话" |
| **Feedback** | 多维评价向量，驱动进化的选择压力 | 效果评分、成本、满意度等多维数据 |

### 进化内核层（v2 增量概念）

| 概念 | 定义 | 作用 |
|---|---|---|
| **DecisionPopulation** | 挂载在 Concept 下的候选方案集合，有固定容量 K | 维护生态位多样性 |
| **EvolTrace** | 变异操作元数据，形成方案谱系树 | 可解释性与审计 |
| **Variator** | 变异算子：LLM Generate / Crossover / Perturb | 产生新方案 |
| **Selector** | 选择算子：Pareto 支配 + 拥挤距离 | 维持种群多样与择优 |
| **Migrator** | 跨概念生态位的方案迁移算子 | 知识复用 |
| **MetaOptimizer** | 调整进化引擎自身超参数 | 元级适应 |

### 概念关系总图

```
InputEvent
    │
    │ classify (LLM Classifier)
    ▼
Concept ──────────────────────────────────────────────────────────────┐
    │ 生态位 (niche)                                                   │
    │                                                                  │
    ├─ DecisionPopulation (K 个 Assignment)                            │
    │      │                                                           │
    │      ├─ Assignment₁ (Decision₁, score 向量, generation=12)      │
    │      ├─ Assignment₂ (Decision₂, score 向量, generation=5)       │
    │      └─ ...                                                     │
    │      │                                                           │
    │      │ 变异 (Variator)                                           │
    │      │  ├─ LLM Generate ──── 注入生态位成功特征摘要              │
    │      │  ├─ Crossover ─────── 亲本₁(谱系A) + 亲本₂(谱系B)       │
    │      │  └─ Perturb ──────── 参数微调                            │
    │      │                                                           │
    │      │ 选择 (Selector)                                           │
    │      │  └─ Pareto 支配 + 拥挤距离保留                            │
    │      │                                                           │
    │      │ 迁移 (Migrator) ── 跨生态位水平迁移 ──► 其他 DecisionPopulation
    │                                                                  │
    ▼                                                                  │
Execution (执行方案) ──────► Feedback (多维向量)                       │
    │                              │                                   │
    │ 信用分配 (CreditAssignment)  │                                   │
    └──────────◄───────────────────┘                                   │
                                                                       │
元进化层 (MetaOptimizer)                                               │
    └─► 调控：变异率、种群容量 K、拥挤度阈值                             │
    └─► 观测：全局前沿质量 (hypervolume)、多样性指数 (Shannon)          │
                                                                       │
本体验证器 (OntologyValidator) ── 所有变异/迁移通过后置验证            │
    └─► TBox 约束：disjointness, domain/range, 自定义规则             │
```

---

## 四、进化内核

### 核心循环

OntoEvolve 的进化循环发生在每个 Concept 生态位中，由混合触发模型驱动：

```
         ┌──────────────────────────────────┐
         │         Evolution Engine          │
         │  (per Concept, 混合触发)          │
         │                                   │
    ┌────┴────┐    ┌──────────┐    ┌───────┴───────┐
    │ Variator │    │ Selector │    │   Migrator    │
    │ 产生变异  ├───►│ Pareto排序├───►│ 跨生态位迁移   │
    └────┬────┘    └──────────┘    └───────┬───────┘
         │                                  │
    ┌────▼────┐                    ┌───────▼───────┐
    │Ontology │                    │  Ontology     │
    │Validator│                    │  Validator    │
    └────┬────┘                    └───────┬───────┘
         │                                  │
         └──────────────┬───────────────────┘
                        │
                 ┌──────▼──────┐
                 │ 种群更新 +   │
                 │ EvolTrace   │
                 └─────────────┘
```

### 混合触发模型

| 层级 | 触发条件 | 操作 | 延迟 | 是否阻塞决策 |
|---|---|---|---|---|
| **微观** | 每条 Feedback | 实时更新 Assignment 统计量（均值/方差） | 同步，O(1) | 否 |
| **中观** | 累积 N 条反馈（如 20） | Pareto 重排 + 拥挤度选择 | 异步，亚秒级 | 否 |
| **宏观** | 定时调度（如每 2h） | LLM 变异 + 重组 + 迁移 | 异步，秒级 | 否 |
| **手动** | API 端点触发 | 完整进化代 | 同步/异步可选 | 按需 |

### 变异算子

```java
public interface Variator<D extends Decision, A extends Assignment<D>> {
    List<A> generate(VariationContext ctx);
}
```

| 算子 | 简介 | 成本 | 执行条件 |
|---|---|---|---|
| **LLM_Generate** | 调用大模型生成全新方案，prompt 注入生态位成功特征摘要 | 高（LLM 调用） | 主动探索率触发或匹配失败 |
| **Crossover** | 从种群选两个高分亲本，LLM 融合生成继承双方特征的新方案 | 高（LLM 调用） | 种群大于 2 |
| **Perturb** | 小幅调整参数化方案的数值属性，无 LLM 调用 | 极低 | 存在参数化方案 |

### 多目标选择

```
多目标反馈向量
  [effectivenessShort, effectivenessLong, cost, satisfaction, sideEffect]

                  Pareto 前沿
                     ▲
        效果 │    A  C                A: 高效高成本
            │  B    D                B: 中效低成本
            │    E                    C: 高效中等成本
            │                         D: 中效极低成本
            └────────────────────►    E: 低效低成本 (被支配)
                    成本
                    
前沿层 0: {A, B, C, D} — 互不支配，各自代表不同权衡
前沿层 1: {E} — 被 B 和 D 同时支配

拥挤距离: 在同一前沿内，保留稀疏区域的方案以维持多样性
```

### 元进化

MetaOptimizer 在系统层面以更慢的节律（如每天）运行：

```
超参数空间: [变异率, 种群容量 K, 拥挤度阈值, UCB 探索系数, ...]

宏观适应度: [平均 hypervolume, 种群多样性指数, 方案废弃率, LLM 调用成本]

优化算法: 贝叶斯优化 或 简单 Hill-Climbing
```

---

## 五、对比

### vs 大语言模型 (LLM)

| 维度 | LLM | OntoEvolve |
|---|---|---|
| 核心能力 | 给定上下文生成最可能的文本 | 在语义生态位中演化方案种群 |
| 知识表示 | 隐式参数 | 显式本体（OWL），可查询可推理 |
| 探索机制 | 无内建探索（依赖 prompt 随机性） | 种群级系统探索：变异+重组+迁移 |
| 方案历史 | 一次性生成，无记忆 | 谱系树累积，代际继承 |
| 安全约束 | 后置护栏 prompt | 本体 TBox 前置硬约束 |
| 可解释性 | 注意力事后归因 | 本体因果链+谱系树一体化 |

**LLM 是"敏捷的发明家"**：能马上创造出看似合理的方案，但无法保证长期效果与安全。OntoEvolve 用 LLM 作为变异引擎（发挥其创造力），同时用本体和进化算法框定其产出，使其创造力服务于可控的进化。

### vs 推荐算法

| 维度 | 推荐算法 | OntoEvolve |
|---|---|---|
| 问题 | 用户→物品匹配，预测评分 | 事件→方案匹配，演化方案设计 |
| 候选池 | 封闭静态 | 开放动态，方案可自创可灭绝 |
| 优化目标 | 单目标（CTR/转化率） | 多目标 Pareto 前沿 |
| 知识积累 | 隐向量/模型参数 | 本体 ABox 中显式可查询 |
| 冷启动 | 利用协同信号 | 依赖 LLM 生成 + 本体迁移 |
| 可解释性 | 事后相似用户/物品解释 | 事前语义约束+事后谱系溯源 |

**推荐算法是"高效的分拣员"**：从海量候选物中极速选出最讨喜的那个。OntoEvolve 是"进化生态学家实验室"：让方案种群自主变异、竞争、迁移，用多目标压力进化出稳健且有谱系的解决方案。

### vs 强化学习 (RL)

| 维度 | RL | OntoEvolve |
|---|---|---|
| 核心问题 | MDP 中最大化累积折扣奖励 | 语义生态位中寻找 Pareto 最优方案 |
| 决策粒度 | 序列决策（每步影响下一状态） | 单步决策 + 方案演化 |
| 状态表示 | 向量/嵌入（黑箱） | OWL 个体（白箱，可推理） |
| 动作空间 | 固定离散/连续集 | 开放动态种群 |
| 安全约束 | 后置惩罚/安全层 | 本体前置硬约束 |
| 泛化方式 | 神经网络插值 | 生态位迁移（本体相似性） |
| 样本效率 | 高（梯度下降） | 低（种群变异） |

**RL 是"黑暗中的试错者"**：从零开始碰壁学会序列选择。OntoEvolve 是"有蓝图的花园育种者"：先绘制安全语义花园，再播种杂交，世代累积有谱系的方案种群。

两者互补：OntoEvolve 可以充当 RL 的"动作设计器"，RL 可以充当 OntoEvolve 的"时序调度器"。

### 决策范式全景图

```
                   高 ▲
                      │    本工作
      可解释性        │  OntoEvolve
                      │    ↑
                      │    │ 本体驱动 + 多目标进化 + LLM 变异引擎
                      │
                      ├── 业务规则引擎 ──── 强化学习
                      │  (Drools/ILOG)     │
                      │       ↑            │ 模型可解释性差
                      │       │ 静态规则    │ 但序列决策能力强
                      │       │ 无进化能力  │
                      │                    │
                      ├── 推荐系统 ────────┤
                      │  (CF/DL)           │ 深度推荐
                      │                    │ 黑箱但高效
                      │                    │
                      │ 多臂老虎机 ────────┘
                      │  (A/B Test)
                      │
                    低 └──────────────────────────►
                      低        序列决策能力        高
```

---

## 六、技术方案图

### 模块架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                          onto-evolve-core                            │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────────┐  │
│  │ 接口定义层    │  │ 元本体模型    │  │ 进化循环引擎                │  │
│  │ Classifier   │  │ InputEvent   │  │ evolve(Concept)            │  │
│  │ Matcher      │  │ Concept      │  │ ├─ Variator                │  │
│  │ Variator     │  │ Decision     │  │ ├─ Selector                │  │
│  │ Selector     │  │ Assignment   │  │ ├─ Migrator                │  │
│  │ Migrator     │  │ Execution    │  │ └─ MetaOptimizer           │  │
│  │ MetaOptimizer│  │ Feedback     │  │                              │  │
│  └─────────────┘  └──────────────┘  └────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
                                    │
         ┌──────────────────────────┼──────────────────────────┐
         │                          │                          │
┌────────▼─────────┐  ┌─────────────▼──────┐  ┌───────────────▼──────┐
│ onto-evolve-plugins │  onto-evolve-infra   │  onto-evolve-starter   │
│                    │                      │                        │
│ LLM Variator      │  RDF 存储适配        │  Spring Boot AutoConfig │
│ Pareto Selector   │  LLM 客户端适配      │  @ConditionalOnProperty  │
│ Migrator          │  Metrics 收集        │  默认装配 + SPI 扩展    │
│ CreditAssigner    │  OntologyValidator   │  YAML 驱动              │
└───────────────────┘  └────────────────────┘  └──────────────────────┘
```

### 六大插件接口

所有核心策略均定义为可替换接口，支持配置指定实现类，领域项目可零代码替换。

```java
// 输入分类器
public interface Classifier<I extends InputEvent, C extends Concept> {
    C classify(I event);
}

// 决策匹配器
public interface Matcher<C extends Concept, D extends Decision, A extends Assignment<D>> {
    A match(C concept, Context ctx);
}

// 变异算子
public interface Variator<D extends Decision, A extends Assignment<D>> {
    List<A> generate(VariationContext ctx);
}

// 选择算子
public interface Selector<A extends Assignment> {
    List<A> select(List<A> population, int capacity);
}

// 迁移算子
public interface Migrator<A extends Assignment> {
    List<A> proposeMigrations(Concept source, Concept target, List<A> sourceElites);
}

// 元进化优化器
public interface MetaOptimizer {
    MetaParams optimize(GlobalMetrics metrics);
}
```

### 配置模型 (YAML)

```yaml
onto:
  rdf:
    store-type: tdb2
    ontology-path: classpath:ontology/education.ttl
    inference: rdfs

  classifier:
    implementation: com.example.LLMActionClassifier
    llm:
      provider: openai
      model: gpt-4
    unknown-concept:
      auto-create: false
      approval-queue: jdbc:queue:concept_approval

  evolution:
    trigger:
      feedback-count:
        per-niche: 20       # 中观：累积反馈触发重排
      schedule:
        cron: "0 0 2 * * ?" # 宏观：每日完整进化代
    population:
      default-capacity: 20
    variators:
      - type: LLM_GENERATE
        weight: 2
        exploration-rate: 0.15
      - type: CROSSOVER
        weight: 5
      - type: PERTURB
        weight: 1
    selector:
      implementation: com.onto.evolve.core.selector.ParetoCrowdingSelector
    migration:
      enabled: true
      compatibility-threshold: 0.85
      check-interval: "P7D"

  meta:
    enabled: true
    parameters: [exploration-rate, population-capacity]
    target-metrics: [average_hypervolume, niche_diversity_index]

  observability:
    evoltrace-store: db
    metrics:
      prometheus:
        enabled: true
```

### 演化架构全景

```
┌──────────┐    ┌─────────────────┐    ┌──────────────────┐
│ 外部事件   │───►│ EventController │───►│ InterventionServ│
│ (REST)   │    │ POST /event     │    │ (领域编排层)     │
└──────────┘    └─────────────────┘    └────────┬─────────┘
                                                │
              ┌─────────────────────────────────┼─────────────────────┐
              │                                 │                     │
        ┌─────▼──────┐                  ┌───────▼────────┐     ┌─────▼──────┐
        │  Classifier │                  │    Matcher      │     │  Optimizer  │
        │ LLM 分类    │                  │ Pareto+UCB     │     │  实时统计量  │
        └─────┬──────┘                  └───────┬────────┘     └─────┬──────┘
              │                                  │                   │
              └──────────────┬───────────────────┘                   │
                             │                                       │
                    ┌────────▼───────────────────────────────────────▼───┐
                    │                  DAO Layer                        │
                    │  AssignmentDao  ActionDao  EvaluationDao          │
                    └────────┬───────────────────────────────────────────┘
                             │
                    ┌────────▼──────────┐       ┌───────────────────────┐
                    │   JenaTdbStore     │◄──────│  EvolutionEngine     │
                    │   (TDB2 + RDF)    │       │  ┌─────────────────┐  │
                    │   ontology.ttl    │       │  │ Variator        │  │
                    └───────────────────┘       │  ├─ LLM Generate   │  │
                                                │  ├─ Crossover      │  │
                    ┌────────────────────┐      │  └─ Perturb        │  │
                    │  MetaOptimizer      │◄─────│  Selector         │  │
                    │  调整超参数         │      │  ├─ Pareto Sort   │  │
                    └────────────────────┘      │  ├─ Crowding Dist  │  │
                                                │  └─ Truncate       │  │
                    ┌────────────────────┐      │  Migrator         │  │
                    │  CreditAssignment  │      │  └─ 跨概念迁移     │  │
                    │  长期信用分配       │      └─────────────────────┘  │
                    └────────────────────┘                               │
                                                                         │
                    ┌──────────────────────────────────────────────────┐  │
                    │             OntologyValidator                    │  │
                    │  TBox 硬约束：disjointness, domain/range, 规则   │◄─┘
                    └──────────────────────────────────────────────────┘
```

---

## 七、领域适配

### 适配步骤

```
Step 1                                      Step 2
┌─────────────────┐                        ┌────────────────────┐
│ 定义领域本体     │                        │ 配置 application.yml│
│ education.ttl   │                        │ 指定本体路径、      │
│ ├─ ActionType   │                        │ 插件实现、         │
│ ├─ Intervention │                        │ 进化参数          │
│ └─ Evaluation   │                        └────────────────────┘
└─────────────────┘
         │                                         │
         └────────────┬────────────────────────────┘
                      │
                ┌─────▼──────┐
                │  启动应用   │
                │  自动装配 + │
                │  本体加载   │
                └────────────┘
                      │
         ┌────────────┼────────────┐
         │            │            │
    ┌────▼────┐  ┌────▼────┐  ┌───▼──────┐
    │ Classifier│  │ Variator │  │ Selector │
    │ (可选实现)│  │ (可选实现)│  │ (可选实现)│
    └──────────┘  └──────────┘  └──────────┘
```

### 领域映射表

| 元本体 | 学生行为干预 | 医疗分诊 | 客服工单 |
|---|---|---|---|
| InputEvent | ActionEvent | SymptomReport | Ticket |
| Concept | ActionType | TriageCategory | IssueType |
| Decision | Intervention | TreatmentPlan | Resolution |
| Assignment | InterventionAssignment | TreatmentAssignment | ResolutionAssignment |
| Execution | InterventionExecution | TreatmentExecution | ResolutionExecution |
| Feedback | Evaluation | Outcome | Satisfaction |

---

## 八、路线图

```
Phase 1: v2 最小验证 (当前 → 3 个月)
  ├── 种群模型：每个 Concept 下维护 K 个 Assignment
  ├── Pareto 选择器：多维排序 + 拥挤距离
  ├── 混合触发：反馈计数重排 + 定时完整进化
  └── 谱系记录：EvolTrace 基础存储

Phase 2: 变异算子链 (3 — 6 个月)
  ├── LLM_Generate：注入生态位特征摘要
  ├── Crossover：亲本选择 + 融合 prompt 模板
  ├── Perturb：参数微调
  └── 变异算子权重配置化

Phase 3: 生态位迁移与知识复用 (6 — 9 个月)
  ├── 跨 Concept 相似性评估（本体 + 嵌入）
  ├── Migrator 接口实现
  └── 迁移兼容性校验（OntologyValidator）

Phase 4: 元进化与生产打磨 (9 — 12 个月)
  ├── MetaOptimizer：超参数自适应
  ├── 多维信用分配（本体因果链指导）
  ├── 管理界面：谱系可视化 + 前沿演进监控
  └── 基准测试套件（多领域模拟环境）
```

---

## 九、研究定位

OntoEvolve 位于四个领域的交叉地带，但又不完全属于任何一个：

```
                ┌──────────────┐
                │   语义网     │
                │  (OWL/SPARQL)│
                └──────┬───────┘
                       │
    ┌──────────┐      │      ┌──────────────┐
    │ 进化计算  │──────┼──────│  LLM 工程    │
    │ (EA/GP)   │     │      │ (API/Prompt)  │
    └──────────┘      │      └──────────────┘
                       │
                ┌──────┴───────┐
                │  决策支持系统 │
                │   (自适应)    │
                └──────────────┘
```

这个交叉方向的"孤独"并非没有原因：它要求同时理解 OWL 推理、进化算法和 LLM 工程。但这种孤独恰恰也是机会——在一个尚未充分开垦的交叉地带，一旦做出可验证的实例，就可能开辟一个新的研究范式。

---

> **OntoEvolve — 让系统在语义的围栏里，自由地进化出更好的决策。**
