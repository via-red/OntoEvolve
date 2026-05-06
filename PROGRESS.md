# OntoEvolve 开发进度

> 项目版本: 0.1.0-SNAPSHOT
> 最后更新: 2026-05-06

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

### 1.6 单元测试

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
| **LLM/Crossover/Perturb 测试** | ⏳ 待完成 | 依赖 ChatClient，需 Mock |

### 2.3 迁移算子

| 实现 | 状态 | 说明 |
|------|------|------|
| SemanticMigrator | ✅ 完成 | 基于父类/祖先/属性重叠度的兼容性评估 |
| SemanticMigrator 测试 | ✅ 完成 | 低兼容度/父子/兄弟/阈值边界 |

### 2.4 匹配器

| 实现 | 状态 | 说明 |
|------|------|------|
| ParetoUCBMatcher | ✅ 完成 | Pareto归一化 + UCB探索奖励的混合策略 |
| **ParetoUCBMatcher 测试** | ⏳ 待完成 | |

### 2.5 元优化器

| 实现 | 状态 | 说明 |
|------|------|------|
| BayesianMetaOptimizer | ✅ 完成 | 爬山法 + 随机扰动（工程简化版） |
| **BayesianMetaOptimizer 测试** | ⏳ 待完成 | |

### 2.6 信用分配器

| 实现 | 状态 | 说明 |
|------|------|------|
| UniformCreditAssigner | ✅ 完成 | 指数衰减 + 时间窗口 |
| OntologyCausalCreditAssigner | ✅ 完成 | 时间衰减 + 因果距离 |
| UniformCreditAssigner 测试 | ✅ 完成 | 无历史/分布/最大回溯 |

---

## 三、基础设施层 (onto-evolve-infra)

### 3.1 LLM

| 模块 | 状态 | 说明 |
|------|------|------|
| LLMClient | ✅ 完成 | Spring AI ChatClient 封装，支持系统提示覆盖 |
| OpenAIClient | ✅ 完成 | 已标记 @Deprecated，由 Spring AI 自动配置取代 |

### 3.2 指标

| 模块 | 状态 | 说明 |
|------|------|------|
| MetricsCollector | ✅ 完成 | 种群大小/反馈数/LLM调用/进化代际/超体积/Shannon多样性 |
| **MetricsCollector 测试** | ⏳ 待完成 | |

### 3.3 存储

| 模块 | 状态 | 说明 |
|------|------|------|
| OntologyStore 接口 | ✅ 完成 | 概念/方案/反馈/SPARQL 全操作定义 |
| Tdb2OntologyStore | ✅ 完成 | SPARQL 查询已实现，本体文件加载已激活（`classpath:` 解析），save 方法为空存根 |
| **Tdb2OntologyStore 测试** | ⏳ 待完成 | 依赖 TDB2 环境 |
| **SPARQL 通用查询** | ⏳ 待完成 | `query()` 抛出 UnsupportedOperationException |

---

## 四、Spring Boot Starter (onto-evolve-starter)

| 模块 | 状态 | 说明 |
|------|------|------|
| OntoEvolveAutoConfiguration | ✅ 完成 | 全 Bean 条件装配，@ConditionalOnMissingBean 支持覆盖，新增 OntologyStore 条件装配 |
| OntoEvolveApplication | ✅ 完成 | 启动入口 |
| application-ontoevolve.yml | ✅ 完成 | 默认配置，含完整注释 |
| **Docker 容器化** | ⏳ 待完成 | |
| **CI/CD 配置** | ⏳ 待完成 | |

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
| LLMActionClassifier | ✅ 完成 | 实现 Classifier SPI 接口，LLM 语义分类，含回退关键词注册机制 |
| InterventionService | ✅ 完成 | 分类→匹配→进化 全链路编排，子概念无匹配时递归回退父概念 |
| EducationOntologyValidator | ✅ 完成 | 基于 Jena Model 的本体验证器，加载 education.ttl，检查 Concept IRI 的 owl:Class 存在性（含祖先回溯） |

### 5.3 API 与前端

| 模块 | 状态 | 说明 |
|------|------|------|
| EventController (REST API) | ✅ 完成 | 6 端点：事件处理/评价提交/种群查看/触发进化/指标/谱系 |
| Dashboard HTML | ✅ 完成 | 4 面板仪表盘：概览/种群/谱系/调试 |

### 5.4 资源文件

| 资源 | 状态 | 说明 |
|------|------|------|
| application.yml | ✅ 完成 | DeepSeek 集成，教育领域参数覆写 |
| classifier.st | ✅ 完成 | 分类 Prompt 模板 |
| variator_crossover.st | ✅ 完成 | 交叉 Prompt 模板 |
| variator_generate.st | ✅ 完成 | 生成 Prompt 模板 |

### 5.5 待办

| 事项 | 状态 | 说明 |
|------|------|------|
| **领域测试** | ⏳ 待完成 | 无任何领域层测试 |
| **Execution 真实创建** | ⏳ 待完成 | `submitEvaluation()` 目前传 null |

---

## 六、待开发的高级功能

| 功能 | 状态 | 说明 |
|------|------|------|
| Prometheus 监控集成 | ⏳ 待完成 | Micrometer 依赖已引入，配置未启用 |
| 迁移时间窗口检查 | ⏳ 待完成 | `checkAndMigrate` 未实现 `checkInterval` 逻辑 |
| 概念谱系递归查询 | ⏳ 待完成 | TDB2 的 `findConcept` 未递归解析父概念 |

---

## 进度统计

| 层级 | 总项 | 已完成 | 部分完成 | 待完成 | 完成率 |
|------|------|--------|----------|--------|--------|
| 核心框架层 | 14 | 14 | 0 | 0 | 100% |
| 插件层 | 16 | 11 | 0 | 5 | 69% |
| 基础设施层 | 8 | 6 | 0 | 2 | 75% |
| Spring Boot Starter | 5 | 3 | 0 | 2 | 60% |
| 教育领域示例 | 12 | 10 | 0 | 2 | 83% |
| 高级功能 | 3 | 0 | 0 | 3 | 0% |
| **合计** | **58** | **44** | **0** | **14** | **76%** |
