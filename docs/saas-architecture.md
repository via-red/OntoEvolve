# OntoEvolve SaaS 平台架构设计

> 一个应用 + 多领域配置：JSON 驱动的通用演化决策平台

---

## 一、设计目标

### 1.1 核心理念

将 OntoEvolve 从"一个 Maven 模块 = 一个领域应用"的模式，转变为**一个平台应用加载多领域 JSON 配置**的模式。领域配置（概念树、决策类型、反馈维度、分类规则、字段定义）全部以 JSON 文件形式存储，平台启动时加载，运行时驱动通用 API 的行为。

### 1.2 关键决策

| 决策 | 选择 | 原因 |
|------|------|------|
| 架构模式 | 单应用 + 多领域配置 | 避免多进程部署的运维复杂度 |
| 配置格式 | JSON 文件 | 简单、版本可控、便于热加载 |
| 数据隔离 | 每个领域独立配置数据库连接 | 真正的物理隔离，满足多租户合规需求 |
| 运行时存储 | 由领域配置决定（memory / neo4j） | memory 用于开发测试，neo4j 用于生产 |
| 前端 | 新建 `frontend-saas/` 项目 | 完全独立，不依赖现有 `frontend/` |
| 与教育领域关系 | 独立模块，参考不依赖 | `onto-domain-education` 保持冻结 |

### 1.3 术语定义

| 术语 | 含义 |
|------|------|
| 领域 (Domain) | 一个独立的业务领域，含完整的概念体系、决策规则、配置 |
| 领域配置 | 描述一个领域的 JSON 文件集合 |
| 工作区 (Workspace) | 用户选定领域后，在该领域内进行操作的空间 |
| 记录 (Record) | 用户提交到领域的原始事件/数据 |
| 演化 (Evolution) | 平台针对种群执行的变异-选择-迁移循环 |

---

## 二、整体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                      OntoEvolve SaaS Platform                        │
│                                                                      │
│  ┌──────────────┐    ┌──────────────────────────────────────────┐   │
│  │  Frontend     │    │  Backend (Spring Boot)                   │   │
│  │  (React SPA)  │    │                                          │   │
│  │               │    │  ┌──────────────────────────────────┐   │   │
│  │  /            │◄──►│  │  Controllers                     │   │   │
│  │   ├─ /admin/* │    │  │  ├─ AdminController  (管理API)    │   │   │
│  │   ├─ /{did}/* │    │  │  └─ DomainController (领域API)    │   │   │
│  │   └─ / (原有)  │    │  └──────────────────────────────────┘   │   │
│  └──────────────┘    │                      │                      │
│                       │                      ▼                      │
│                       │  ┌──────────────────────────────────┐   │   │
│                       │  │  Services                        │   │   │
│                       │  │  ├─ DomainConfigService          │   │   │
│                       │  │  ├─ DomainOrchestrationService   │   │   │
│                       │  │  └─ 复用: EvolutionEngine 等     │   │   │
│                       │  └──────────────────────────────────┘   │   │
│                       │                      │                      │
│                       │                      ▼                      │
│                       │  ┌──────────────────────────────────┐   │   │
│                       │  │  Storage                         │   │   │
│                       │  │  ├─ data/domains/*/ (JSON配置)   │   │   │
│                       │  │  └─ DomainStoreFactory           │   │   │
│                       │  │       ├─ Domain A ─→ Neo4j DB-A  │   │   │
│                       │  │       ├─ Domain B ─→ In-Memory   │   │   │
│                       │  │       └─ Domain C ─→ Neo4j DB-C  │   │   │
│                       │  └──────────────────────────────────┘   │   │
│                       └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.1 模块结构

```
onto-evolve-parent (pom.xml)
│
├── onto-evolve-core          ← 核心模型 + SPI（不变）
├── onto-evolve-infra         ← LLM、Jena、指标（不变）
├── onto-evolve-plugins       ← 默认 SPI 实现（不变）
├── onto-evolve-starter       ← 自动装配（不变）
├── onto-evolve-graph-store   ← Neo4j（不变）
├── onto-domain-education     ← 教育领域（冻结，不再修改）
│
└── onto-evolve-saas (新增)    ← SaaS 平台模块
    ├── config/               ← JSON 配置模型
    ├── service/              ← 领域配置加载、通用编排
    ├── controller/           ← 管理 API + 领域 API
    └── SaasApplication.java  ← 平台入口
```

### 2.2 依赖关系

```
onto-evolve-saas
  ├── onto-evolve-starter     ← 复用全部自动装配
  ├── spring-boot-starter-web ← REST API
  └── jackson-databind        ← JSON 解析（Spring Boot 自带）
```

不需要 `onto-domain-education` 的任何代码。SaaS 模块通过 `@ComponentScan` 扫描 starter 和自身的包。

---

## 三、JSON 配置数据模型

### 3.1 存储结构

每个领域在 `data/domains/{domain-id}/` 目录下有一组 JSON 文件：

```
data/domains/
  education/                ← 教育领域（参考配置）
    domain.json             ← 领域元信息 + LLM 配置
    ontology.json           ← 概念树（层次结构）
    decisions.json          ← 决策类型定义
    feedback.json           ← 反馈维度定义
    rules.json              ← 分类规则
    fields.json             ← 事件记录字段定义
```

### 3.2 各文件 schema

#### `domain.json`

```json
{
  "id": "education",
  "name": "学生行为干预",
  "description": "学生行为管理与干预方案推荐",
  "namespace": "http://ontoevolve/education#",
  "status": "PUBLISHED",
  "llm": {
    "provider": "openai",
    "model": "deepseek-v4-flash"
  },
  "store": {
    "type": "neo4j",
    "config": {
      "uri": "bolt://localhost:7687",
      "database": "ontoevolve_education",
      "username": "neo4j",
      "password": "${NEO4J_PASSWORD}"
    }
  }
}
```

**`store` 配置说明：**

`store.type` 决定了该领域的运行时数据存到哪里：

| type | 说明 | 适用场景 |
|------|------|----------|
| `memory` | 进程内存，重启即失 | 开发测试、演示 |
| `neo4j` | 独立 Neo4j 数据库 | 生产环境，需要持久化和图查询 |

每个领域可以指向完全不同的数据库实例，实现物理层面的数据隔离。`store.config` 中的连接参数支持 `${ENV_VAR}` 环境变量替换，避免明文密码。

#### `ontology.json`

```json
{
  "concepts": [
    {
      "iri": "Behavioral",
      "label": "行为问题",
      "parentIri": null,
      "properties": {
        "color": "#d97706",
        "icon": "behavior"
      },
      "constraints": {
        "disjointWith": ["Academic", "Social"]
      },
      "sortOrder": 1
    },
    {
      "iri": "ClassroomDisruption",
      "label": "课堂扰乱",
      "parentIri": "Behavioral",
      "properties": {
        "color": "#f59e0b"
      },
      "constraints": {},
      "sortOrder": 1
    },
    {
      "iri": "Academic",
      "label": "学业问题",
      "parentIri": null,
      "properties": {
        "color": "#059669",
        "icon": "academic"
      },
      "constraints": {
        "disjointWith": ["Behavioral", "Social"]
      },
      "sortOrder": 2
    },
    {
      "iri": "HomeworkMissing",
      "label": "缺交作业",
      "parentIri": "Academic",
      "properties": {
        "color": "#10b981"
      },
      "constraints": {},
      "sortOrder": 1
    },
    {
      "iri": "Social",
      "label": "社交问题",
      "parentIri": null,
      "properties": {
        "color": "#6366f1",
        "icon": "social"
      },
      "constraints": {
        "disjointWith": ["Behavioral", "Academic"]
      },
      "sortOrder": 3
    }
  ]
}
```

#### `decisions.json`

```json
{
  "decisionTypes": [
    {
      "iri": "TalkIntervention",
      "name": "一对一谈话",
      "description": "与学生进行一对一沟通",
      "paramsSchema": {
        "type": "object",
        "properties": {
          "duration": { "type": "number", "description": "谈话时长（分钟）" },
          "followUp": { "type": "boolean", "description": "是否需要后续跟进" }
        }
      },
      "stepsTemplate": [
        "了解学生情况",
        "指出问题行为",
        "共同制定改进计划"
      ]
    },
    {
      "iri": "NoticeIntervention",
      "name": "通知家长",
      "description": "联系家长告知情况",
      "paramsSchema": {},
      "stepsTemplate": [
        "准备情况说明",
        "电话或当面沟通",
        "记录家长反馈"
      ]
    },
    {
      "iri": "RewardIntervention",
      "name": "正向激励",
      "description": "通过奖励强化积极行为",
      "paramsSchema": {
        "type": "object",
        "properties": {
          "rewardType": {
            "type": "string",
            "enum": ["表扬信", "积分奖励", "荣誉称号"]
          }
        }
      },
      "stepsTemplate": [
        "确认积极行为",
        "选择奖励方式",
        "实施奖励"
      ]
    },
    {
      "iri": "CounselingIntervention",
      "name": "心理辅导",
      "description": "转介心理辅导",
      "paramsSchema": {},
      "stepsTemplate": [
        "评估心理状态",
        "联系心理老师",
        "安排辅导计划"
      ]
    },
    {
      "iri": "AcademicSupport",
      "name": "学业辅导",
      "description": "提供额外学业支持",
      "paramsSchema": {
        "type": "object",
        "properties": {
          "subject": { "type": "string", "description": "辅导科目" },
          "hoursPerWeek": { "type": "number", "description": "每周辅导时长" }
        }
      },
      "stepsTemplate": [
        "评估学业短板",
        "制定辅导计划",
        "定期检查进度"
      ]
    },
    {
      "iri": "ActivityIntervention",
      "name": "活动引导",
      "description": "通过集体活动改善行为",
      "paramsSchema": {
        "type": "object",
        "properties": {
          "activityType": {
            "type": "string",
            "enum": ["团队建设", "志愿服务", "兴趣小组"]
          }
        }
      },
      "stepsTemplate": [
        "选择合适的活动",
        "安排学生参与",
        "观察并反馈"
      ]
    }
  ],
  "conceptMappings": [
    { "conceptIri": "Behavioral", "decisionIris": ["TalkIntervention", "NoticeIntervention", "RewardIntervention", "CounselingIntervention"], "autoDerive": false },
    { "conceptIri": "Academic", "decisionIris": ["AcademicSupport", "TalkIntervention", "NoticeIntervention"], "autoDerive": false },
    { "conceptIri": "Social", "decisionIris": ["ActivityIntervention", "CounselingIntervention", "TalkIntervention"], "autoDerive": false }
  ]
}
```

**`conceptMappings` 说明**：
- `autoDerive: false`：使用显式配置的 mapping
- `autoDerive: true`：平台自动推导该概念可用哪些决策类型（基于概念相似度或继承链）

#### `feedback.json`

```json
{
  "dimensions": [
    {
      "name": "effectiveness",
      "label": "干预效果",
      "description": "干预是否有效改善了行为",
      "min": 0,
      "max": 1,
      "higherIsBetter": true,
      "sortOrder": 1
    },
    {
      "name": "cost",
      "label": "资源消耗",
      "description": "执行方案的资源消耗程度",
      "min": 0,
      "max": 1,
      "higherIsBetter": false,
      "sortOrder": 2
    },
    {
      "name": "satisfaction",
      "label": "满意度",
      "description": "学生和教师对方案的接受度",
      "min": 0,
      "max": 1,
      "higherIsBetter": true,
      "sortOrder": 3
    }
  ]
}
```

平台根据 `dimensions` 列表的长度决定 `scoreVector` 的维度。提交反馈时，按 `sortOrder` 顺序依次传入分数。

#### `rules.json`

```json
{
  "classificationRules": [
    {
      "type": "LLM",
      "priority": 1,
      "promptTemplate": "你是一个分类器。请将以下行为描述分类到最合适的类别中。\n\n事件描述：{{description}}\n严重程度：{{severity}}\n\n可选类别：\n{{typeList}}\n\n请仅输出类别的 IRI。",
      "conceptIri": null,
      "fallbackStrategy": "AUTO_CREATE"
    },
    {
      "type": "KEYWORD",
      "priority": 2,
      "keywords": ["作业", "考试", "成绩", "上课"],
      "conceptIri": "Academic",
      "fallbackStrategy": "REJECT"
    }
  ]
}
```

`conceptIri: null` 表示该规则由 LLM 自主选择分类，不预设目标。多个规则按 `priority` 升序执行，先匹配者优先。

#### `fields.json`

```json
{
  "fieldDefinitions": [
    {
      "fieldName": "description",
      "fieldType": "STRING",
      "label": "行为描述",
      "required": true,
      "sortOrder": 1
    },
    {
      "fieldName": "severity",
      "fieldType": "ENUM",
      "label": "严重程度",
      "required": true,
      "enumValues": ["mild", "moderate", "severe"],
      "sortOrder": 2
    },
    {
      "fieldName": "location",
      "fieldType": "STRING",
      "label": "发生地点",
      "required": false,
      "sortOrder": 3
    },
    {
      "fieldName": "studentId",
      "fieldType": "STRING",
      "label": "学生ID",
      "required": true,
      "sortOrder": 4
    }
  ]
}
```

---

## 四、通用 API 设计

### 4.1 管理 API (`/api/admin/domains`)

用于前端管理控制台：创建、编辑、发布领域。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/domains` | 列出所有领域 |
| POST | `/api/admin/domains` | 创建新领域（返回初始 JSON 结构） |
| GET | `/api/admin/domains/{id}` | 获取领域元信息 |
| PUT | `/api/admin/domains/{id}` | 更新领域元信息 |
| DELETE | `/api/admin/domains/{id}` | 删除领域 |
| POST | `/api/admin/domains/{id}/publish` | 发布领域（设为可用） |
| POST | `/api/admin/domains/{id}/unpublish` | 下架领域 |
| GET | `/api/admin/domains/{id}/config` | 获取领域完整配置（JSON 汇总） |
| PUT | `/api/admin/domains/{id}/ontology` | 替换概念配置 |
| PUT | `/api/admin/domains/{id}/decisions` | 替换决策类型配置 |
| PUT | `/api/admin/domains/{id}/mappings` | 替换映射配置 |
| PUT | `/api/admin/domains/{id}/feedback` | 替换反馈维度配置 |
| PUT | `/api/admin/domains/{id}/rules` | 替换分类规则配置 |
| PUT | `/api/admin/domains/{id}/fields` | 替换字段定义配置 |

### 4.2 领域 API (`/{domainId}/...`)

用户选定领域后的工作区 API。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/{domainId}/records` | 提交一条记录（事件） |
| GET | `/{domainId}/records` | 列出记录（分页、按字段过滤） |
| GET | `/{domainId}/records/{id}` | 记录详情（包含分类结果和匹配决策） |
| POST | `/{domainId}/records/{id}/feedback` | 提交反馈（scores: number[]） |
| GET | `/{domainId}/ontology` | 获取概念树 |
| GET | `/{domainId}/populations` | 列出所有生态位的种群 |
| GET | `/{domainId}/populations/{conceptIri}` | 某概念下的种群详情 + Pareto 前沿 |
| POST | `/{domainId}/evolve` | 手动触发某概念生态位的完整进化 |
| GET | `/{domainId}/graph` | 关系图谱（生态位和记录的关系网络） |
| GET | `/{domainId}/metrics` | 系统指标（LLM 调用、种群状态等） |

### 4.3 通用请求/响应格式

**提交记录请求：**
```json
POST /education/records
{
  "description": "学生上课大声喧哗，影响其他同学",
  "severity": "moderate",
  "location": "3楼教室",
  "studentId": "S10023"
}
```

字段根据 `fields.json` 定义校验。后端返回 `Map<String, Object>`，其中固定字段包含 `id`、`classifiedConcept`、`matchedDecision`，其余为领域自定义字段。

**提交反馈请求：**
```json
POST /education/records/r-001/feedback
{
  "scores": [0.8, 0.3, 0.9]
}
```

`scores` 数组顺序对应 `feedback.json` 中 dimensions 的 `sortOrder` 顺序。这里：0.8=effectiveness, 0.3=cost, 0.9=satisfaction。

---

## 五、数据隔离与存储架构

### 5.1 控制面 vs 数据面

平台将配置和运行时数据严格分离：

```
┌──────────────────────────────────────────────────────────┐
│                    控制面 (Control Plane)                  │
│  所有领域共享一份：data/domains/{id}/*.json               │
│  领域元信息、概念树、决策类型、规则、字段定义             │
├──────────────────────────────────────────────────────────┤
│                    数据面 (Data Plane)                     │
│  每个领域独立配置存储后端：                                │
│                                                            │
│  Domain A (education)  ──→ Neo4J: bolt://host:7687/db-a   │
│  Domain B (healthcare) ──→ Neo4J: bolt://other:7687/db-b  │
│  Domain C (testing)    ──→ In-Memory (仅进程内存)         │
│                                                            │
│  数据面包含：                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ • Events/Records （原始事件记录）                    │   │
│  │ • Populations & Assignments（种群和分配方案）        │   │
│  │ • Executions（方案执行记录）                         │   │
│  │ • Feedback（反馈评分）                               │   │
│  │ • Evolution Traces（演化轨迹）                       │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────┘
```

### 5.2 Store 抽象层

平台定义 `DomainStore` SPI，每个领域启动时由 `DomainStoreFactory` 根据 `domain.json` 的 `store` 配置创建对应的实例：

```java
public interface DomainStore extends PopulationStore {
    // 继承 PopulationStore 的所有方法
    // 额外增加：
    void saveRecord(String domainId, InputEvent record);
    List<InputEvent> getRecords(String domainId, Map<String, Object> filters);
    Optional<InputEvent> getRecord(String domainId, String recordId);
    void saveFeedback(String domainId, Feedback feedback);
    // ...
}
```

**内置实现：**

| 实现 | 配置 `store.type` | 说明 |
|------|-------------------|------|
| `MemoryDomainStore` | `memory` | ConcurrentHashMap 存储，进程重启即失 |
| `Neo4jDomainStore` | `neo4j` | Neo4j 持久化，每个 domainId 对应一个数据库 |

### 5.3 运行时 Store 管理

`DomainStoreManager` 是中心化的 store 注册表：

```
平台启动
  │
  ├── DomainConfigService 扫描 data/domains/
  │    加载所有 status=PUBLISHED 的领域
  │
  ├── DomainStoreFactory 为每个领域创建 DomainStore
  │    ├── 读取 domain.json → store.type → store.config
  │    ├── 创建连接（Neo4j 驱动 或 Memory 实例）
  │    └── 注册到 DomainStoreManager
  │
  └── 请求到达 /{domainId}/records
        ├── DomainOrchestrationService 从 Manager 获取对应 store
        └── 所有读写操作经过该 store
```

### 5.4 Neo4j 多数据库支持

对于 Neo4j 后端，每个领域使用独立的 Neo4j 数据库（Neo4j 4.0+ 支持在一个服务实例中管理多个数据库）：

- 每个领域在 `domain.json` 中配置 `store.config.database` 指定数据库名
- 平台连接 Neo4j 后，通过 `session.executeWrite(tx -> tx.run("USE {dbName} ..."))` 切换数据库
- 数据库由平台首次使用时自动创建

### 5.5 数据面生命周期

| 事件 | 行为 |
|------|------|
| 领域发布 | 创建 DomainStore 实例，连接配置的数据库 |
| 领域下架 | 关闭 DomainStore 连接，释放资源 |
| 领域配置更新 | 仅影响控制面，数据面不受影响 |
| 平台重启 | 控制面重新加载 JSON，数据面重新连接各数据库 |
| 领域删除 | 可选：保留数据 or 清空数据面 |

---

## 六、与教育领域的关系

```
onto-domain-education (冻结)
  └── 参考实现：展示标准 SPI 用法、TTL 本体定义、Spring Boot 集成模式
      │
      ▼ (模式参考，不依赖代码)
onto-evolve-saas (新增)
  └── SaaS 平台：JSON 配置驱动，通用 API，多领域支持
      │
      ├── data/domains/education/  ← 教育领域的 JSON 配置
      │   （与 onto-domain-education 功能等价，但行为由配置驱动）
      └── data/domains/healthcare/ ← 新领域
```

- `onto-domain-education` 不再修改，作为开发者的参考示例
- `onto-evolve-saas` 的 `data/domains/education/` 配置与 `onto-domain-education` 功能等价
- 后端和前端两个应用各司其职：
  - `onto-domain-education` 走原有端口（8088）
  - `onto-evolve-saas` 走新端口（如 8090）

---

## 七、开发计划

| 阶段 | 内容 | 预计产出 |
|------|------|----------|
| P0 | 新建 `onto-evolve-saas` 模块，实现 JSON 配置加载 | 可启动的空平台 |
| P1 | 实现 GenericController (`/{domainId}/records`) | 通用记录提交/查询 API |
| P2 | 实现概念树服务和图谱 API | 通用概念树 + 关系图谱 |
| P3 | 集成 EvolutionEngine，实现完整决策循环 | 自动分类 → 匹配 → 反馈 → 演化 |
| P4 | 前端：领域选择 + admin 配置页面 | 管理员可在线创建/编辑领域 |
| P5 | 前端：通用工作区页面 | 用户选择领域后操作 |
| P6 | Docker 部署（Dockerfile + docker-compose profile） | `docker compose --profile saas up` |
