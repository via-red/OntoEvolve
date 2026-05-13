# OntoEvolve 开发进度

> 项目版本: 0.2.0
> 最后更新: 2026-05-13 (产品化重构版)

---

## 一、核心框架层 (onto-evolve-core)

### 1.1 领域模型

| 模块 | 状态 | 说明 |
|------|------|------|
| Assignment — 方案分配 | ✅ 完成 | 多维评分、Welford 在线方差算法、生命周期状态 (NEWBORN/ACTIVE/PROBATION/DEPRECATED/ELITE) |
| Decision — 决策方案 | ✅ 完成 | IRI 标识、步骤列表、亲本引用 |
| Concept — 本体概念 | ✅ 完成 | is-a 层次、属性映射 |
| Execution — 执行记录 | ✅ 完成 | 反馈锚点，记录执行上下文 |
| Feedback — 反馈评价 | ✅ 完成 | 多维奖励向量，驱动进化选择压力 |
| InputEvent — 域外输入 | ✅ 完成 | 决策循环起点 |

### 1.2 SPI 接口

| 接口 | 状态 | 说明 |
|------|------|------|
| Selector | ✅ 完成 | 多目标适应度选择 |
| Variator | ✅ 完成 | 变异算子，产生新方案 |
| Migrator | ✅ 完成 | 跨生态位迁移 |
| Classifier | ✅ 完成 | 输入事件→本体概念映射 |
| CreditAssigner | ✅ 完成 | 延迟反馈信用分配 |
| Matcher | ✅ 完成 | 从种群匹配最优方案 |
| MetaOptimizer | ✅ 完成 | 进化超参数自适应 |
| OntologyValidator | ✅ 完成 | 语义安全验证闸 |
| Environment | ✅ 完成 | 评估环境抽象，支持人工/模拟评估 |
| EnvironmentConfig | ✅ 完成 | 环境配置（模拟模式/噪声/偏差） |

### 1.3 辅助 SPI 类型

| 类型 | 状态 | 说明 |
|------|------|------|
| VariationContext | ✅ 完成 | 变异上下文快照 |
| GlobalMetrics | ✅ 完成 | 全局系统指标 |

### 1.4 内核

| 模块 | 状态 | 说明 |
|------|------|------|
| EvolutionEngine | ✅ 完成 | 变异-选择-迁移循环，轻重双进化模式，权重驱动算子路由，MetaOptimizer 接入轻进化路径 |
| DecisionPopulation | ✅ 完成 | 容量控制、代际追踪、反馈累积触发逻辑 |
| EvolTrace | ✅ 完成 | 完整谱系追踪，支撑可解释性与审计 |

### 1.5 配置与验证

| 模块 | 状态 | 说明 |
|------|------|------|
| OntoEvolveConfig | ✅ 完成 | 全量配置模型，映射 YAML |
| OntologyValidator 默认实现 | ✅ 完成 | 默认始终返回 true，由领域项目覆盖 |

### 1.6 PopulationStore SPI

| 类型 | 状态 | 说明 |
|------|------|------|
| PopulationStore 接口 | ✅ 完成 | 种群/反馈/轨迹读写抽象，解耦 EvolutionEngine 与存储后端 |
| InMemoryPopulationStore | ✅ 完成 | 默认实现，ConcurrentHashMap + synchronizedList，EvolutionEngine 原内存逻辑提取 |

### 1.7 单元测试

| 测试类 | 状态 | 说明 |
|--------|------|------|
| AssignmentTest | ✅ 完成 | 初始化/分数更新/维度校验/状态管理 |
| DecisionPopulationTest | ✅ 完成 | 空种群/添加成员/代数/替换/触发检测 |
| EvolutionEngineTest | ✅ 完成 | 种群创建/反馈追踪/进化代/轨迹记录 |

---

## 二、插件层 (onto-evolve-plugins)

### 2.1 选择算子

| 实现 | 状态 | 说明 |
|------|------|------|
| ParetoCrowdingSelector | ✅ 完成 | NSGA-II 风格：快速非支配排序 + 拥挤度距离 |
| ParetoCrowdingSelector 测试 | ✅ 完成 | 空输入/容量内/Pareto前沿/多样性保持/单维度 |

### 2.2 变异算子

| 实现 | 状态 | 说明 |
|------|------|------|
| LLMGenerateVariator | ✅ 完成 | LLM 全新生成，注入生态位摘要 |
| CrossoverVariator | ✅ 完成 | 双亲本 LLM 融合重组 |
| PerturbVariator | ✅ 完成 | 无 LLM 局部搜索：数值替换/修饰词 |
| **PerturbVariator 测试** | ✅ 完成 | 空种群/子代继承/步骤微扰/类型返回 |
| **LLMGenerateVariator 测试** | ⏳ 待完成 | 依赖 ChatClient，需 Mock |
| **CrossoverVariator 测试** | ⏳ 待完成 | 依赖 ChatClient，需 Mock |

### 2.3 迁移算子

| 实现 | 状态 | 说明 |
|------|------|------|
| SemanticMigrator | ✅ 完成 | 基于父类/祖先/属性重叠度的兼容性评估 |
| SemanticMigrator 测试 | ✅ 完成 | 低兼容度/父子/兄弟/阈值边界 |

### 2.4 匹配器

| 实现 | 状态 | 说明 |
|------|------|------|
| ParetoUCBMatcher | ✅ 完成 | Pareto归一化 + UCB探索奖励的混合策略 |
| **ParetoUCBMatcher 测试** | ✅ 完成 | 空种群/单候选/高分优先/探索奖励/默认构造 |

### 2.5 元优化器

| 实现 | 状态 | 说明 |
|------|------|------|
| BayesianMetaOptimizer | ✅ 完成 | 爬山法 + 随机扰动（工程简化版） |
| **BayesianMetaOptimizer 测试** | ✅ 完成 | 默认参数/多次优化/边界钳制/停滞降级 |

### 2.6 信用分配器

| 实现 | 状态 | 说明 |
|------|------|------|
| UniformCreditAssigner | ✅ 完成 | 指数衰减 + 时间窗口 |
| OntologyCausalCreditAssigner | ✅ 完成 | 时间衰减 + 因果距离 |
| UniformCreditAssigner 测试 | ✅ 完成 | 无历史/分布/最大回溯 |

### 2.7 评估环境

| 实现 | 状态 | 说明 |
|------|------|------|
| Environment SPI | ✅ 完成 | 评估执行效果的抽象接口 |
| SimulatedEvaluationEnvironment | ✅ 完成 | 启发式 + 高斯噪声模拟评分，支持离线测试 |

---

## 三、基础设施层 (onto-evolve-infra)

### 3.1 LLM

| 模块 | 状态 | 说明 |
|------|------|------|
| LLMClient | ✅ 完成 | @Retryable 重试、token 统计、延迟追踪、并发 batch、fallback |
| LLMResponse | ✅ 完成 | 完整 LLM 响应（content + token + finishReason + latencyMs） |
| PromptTemplateService | ✅ 完成 | .st 模板加载与渲染，解耦 prompt 与代码 |
| OpenAiChatConfig | ✅ 完成 | Spring AI OpenAI 配置 |
| **JenaOntologyValidator** | ✅ 完成 | Jena OntModel 驱动的本体验证器：类存在性、disjointness、一致性检查 |

### 3.2 指标

| 模块 | 状态 | 说明 |
|------|------|------|
| MetricsCollector | ✅ 完成 | 种群大小/反馈数/LLM调用/进化代际/超体积/Shannon多样性 |
| **MetricsCollector 测试** | ✅ 完成 | 初始零值/种群记录/反馈/LLM调用/超体积/多样性/快照 |

---

## 四、图存储层 (onto-evolve-graph-store)

### 4.1 Neo4j @Node 实体

| 节点 | 状态 | 关键关系 |
|------|------|----------|
| ActionTypeNode | ✅ 完成 | `(:Parent)-[:SUBSUMES]->(:Child)` 本体层次 |
| InterventionNode | ✅ 完成 | 干预措施节点，`HAS_PARENT` 谱系引用 |
| AssignmentNode | ✅ 完成 | `FOR_CONCEPT→ActionType` · `DECIDES→Intervention` · `HAS_PARENT→Assignment` |
| ExecutionNode | ✅ 完成 | `EXECUTES→Assignment`，关联执行者与目标 |
| EvaluationNode | ✅ 完成 | `EVALUATES→Execution`，三维评分 [effectiveness, cost, satisfaction] |
| ActionEventNode | ✅ 完成 | `CLASSIFIED_AS→ActionType`，事件源头 |
| EvolTraceNode | ✅ 完成 | `PRODUCED→Assignment` · `PRODUCED_DECISION→Intervention` · `DERIVED_FROM→Assignments` |

### 4.2 Repository 层

| 仓库 | 状态 | 说明 |
|------|------|------|
| 7 个 Neo4j Repository | ✅ 完成 | Spring Data Neo4j `@Query` Cypher 查询 |

### 4.3 核心组件

| 组件 | 状态 | 说明 |
|------|------|------|
| ModelMapper | ✅ 完成 | 核心 POJO ↔ Neo4j @Node 双向转换 |
| Neo4jPopulationStore | ✅ 完成 | 实现 PopulationStore SPI：ConcurrentHashMap 热缓存 + Neo4j 写穿持久化 |
| GraphStoreAutoConfiguration | ✅ 完成 | `@ConditionalOnProperty` 控制，仅 store-type=neo4j 时激活 |

---

## 五、Spring Boot Starter (onto-evolve-starter)

| 模块 | 状态 | 说明 |
|------|------|------|
| OntoEvolveAutoConfiguration | ✅ 完成 | 全 Bean 条件装配，@ConditionalOnMissingBean 支持覆盖，PopulationStore 双模式注入 |
| OntoEvolveApplication | ✅ 完成 | 启动入口 |
| application-ontoevolve.yml | ✅ 完成 | 默认配置，含完整注释，双存储模式配置 |
| **Docker 容器化** | ✅ 完成 | docker-compose.yml 含 app + Neo4j 服务 |
| **CI/CD 配置** | ✅ 部分完成 | .github/workflows/maven.yml：内存模式 + Neo4j 模式双构建矩阵，需配置 GitHub Secrets |

---

## 五、教育领域示例 (onto-domain-education)

### 5.1 领域模型

| 模型 | 状态 | 说明 |
|------|------|------|
| ActionEvent | ✅ 完成 | 学生行为事件，Core InputEvent 子类 |
| ActionType | ✅ 完成 | 行为类型生态位，Core Concept 子类 |
| Evaluation | ✅ 完成 | 干预效果四维评价，Core Feedback 子类 |
| Intervention | ✅ 完成 | 干预方案，Core Decision 子类 |

### 5.2 服务层

| 服务 | 状态 | 说明 |
|------|------|------|
| EducationOntologyService | ✅ 完成 | 6 种内置行为类型，内存驱动，支持动态注册 |
| LLMActionClassifier | ✅ 完成 | 实现 Classifier SPI 接口，使用 PromptTemplateService 加载 classifier.st，含回退机制 |
| InterventionService | ✅ 完成 | 全链路编排 + Environment 自动评估 + CreditAssigner 延迟信用分配 |
| EducationOntologyValidator | ✅ 完成 | 加载 education.ttl + 委托 JenaOntologyValidator 进行推理层检查（disjointness/一致性） |

### 6.3 API 与前端

| 模块 | 状态 | 说明 |
|------|------|------|
| EventController (REST API) | ✅ 完成 | 15 端点：Dashboard/Events(分页筛选+详情)/Interventions(全景列表+谱系)/Graph(实例关系图)/Populations(含Pareto摘要)/Evolve/Ontology(含计数)/Traces/Metrics/Students。支持双存储模式 |
| React 前端 | ✅ 完成 | 6 页面产品化 UI：工作台(Dashboard)/事件追溯(Events)/方案库(Interventions)/关系图谱(Graph)/进化监控(Evolution)/文档中心(Docs)。TypeScript + Vite + Recharts |
| 前端产品化重构 (v0.2.0) | ✅ 完成 | 后端新增6端点+增强3端点，前端3重写+2新增+1增强，25测试全部通过 |

### 6.4 资源文件

| 资源 | 状态 | 说明 |
|------|------|------|
| application.yml | ✅ 完成 | DeepSeek 集成，教育领域参数覆写，双存储配置 |
| classifier.st | ✅ 完成 | 分类 Prompt 模板 |
| variator_crossover.st | ✅ 完成 | 交叉 Prompt 模板 |
| variator_generate.st | ✅ 完成 | 生成 Prompt 模板 |

### 6.5 Docker 部署

| 组件 | 状态 | 说明 |
|------|------|------|
| docker-compose.yml | ✅ 完成 | 含 Neo4j 5 服务（bolt:7687, http:7474）+ 健康检查 |
| Neo4j 环境配置 | ✅ 完成 | SPRING_NEO4J_* 环境变量，ONTO_GRAPH_STORE_TYPE=neo4j |

### 6.6 v0.2.0 产品化重构 (2026-05-13)

| 变更 | 说明 |
|------|------|
| 导航重构 | 7页面→6模块：工作台/事件追溯/方案库/关系图谱/进化监控/文档中心 |
| 工作台 Dashboard | 新增：今日事件/待评价/活跃方案/进化代次统计卡片，最近活动时间线，方案效果排行，生态位健康概览 |
| 事件追溯 Events | 重写：分页筛选列表、展开式完整处理链路(事件→分类→方案→评价)、折叠表单 |
| 方案库 Interventions | 新增：跨生态位卡片网格、筛选排序、谱系侧面板(祖先/后代/产生方式) |
| 关系图谱 Graph | 新增：左本体树+右力导向实例关系图(SVG)、节点点击详情 |
| 进化监控 Evolution | 增强：+Pareto前沿散点图(Recharts)、+变异算子统计柱状图、+种群平均效果概览 |
| 文档中心 Docs | 重写(原HowItWorks)：系统介绍/用户手册/本体参考/更新日志四章节 |
| 后端 API | 新增6端点(dashboard/events分页/event详情/interventions列表/lineage谱系/graph实例图)+增强3端点(ontology计数/populations摘要/population详情Pareto) |
| 代码清理 | 删除 Students.tsx/Ontology.tsx/Metrics.tsx/HowItWorks.tsx，功能合并到新模块 |

### 6.7 待办

| 事项 | 状态 | 说明 |
|------|------|------|
| React 前端设计指导 | ✅ 完成 | docs/frontend-design.md：技术栈/配色系统/组件规范/API 端点/代码约定 |
| **领域测试** | ✅ 完成 | EducationOntologyServiceTest（11 测试）、InterventionServiceTest（7 测试）、EducationOntologyValidatorTest（7 测试），合计 25 测试全部通过 |

---

## 七、待开发的高级功能

| 功能 | 状态 | 说明 |
|------|------|------|
| Prometheus 监控集成 | ⏳ 待完成 | Micrometer 依赖已引入，配置未启用 |
| 迁移时间窗口检查 | ✅ 完成 | `checkAndMigrate` 已实现 checkInterval 逻辑，支持 Duration 配置 |

---

## 进度统计

| 层级 | 总项 | 已完成 | 部分完成 | 待完成 | 完成率 |
|------|------|--------|----------|--------|--------|
| 核心框架层 | 18 | 18 | 0 | 0 | 100% |
| 插件层 | 18 | 16 | 0 | 2 | 89% |
| 基础设施层 | 7 | 7 | 0 | 0 | 100% |
| 图存储层 | 11 | 11 | 0 | 0 | 100% |
| Spring Boot Starter | 5 | 5 | 0 | 0 | 100% |
| 教育领域示例 | 20 | 20 | 0 | 0 | 100% |
| 高级功能 | 2 | 1 | 0 | 1 | 50% |
| 产品化重构 (v0.2.0) | 9 | 9 | 0 | 0 | 100% |
| **合计** | **88** | **85** | **0** | **3** | **97%** |
