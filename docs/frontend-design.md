# OntoEvolve 前端设计指导

## 1. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| React | ^18.3 | UI 框架 |
| TypeScript | ^5.5 | 类型安全 |
| Vite | ^5.4 | 构建工具 |
| React Router | ^6.26 | 路由管理 |
| Recharts | ^2.12 | 图表可视化 |

## 2. 项目结构

```
frontend/src/
├── api.ts              # API 客户端，封装所有后端调用
├── App.tsx             # 应用入口，侧栏导航 + 路由定义
├── main.tsx            # ReactDOM 挂载
├── index.css           # 全局样式（CSS 变量主题）
├── index.html          # HTML 入口
├── types/
│   └── index.ts        # TypeScript 类型定义
└── pages/
    ├── Dashboard.tsx   # 仪表盘 — 系统指标概览
    ├── Events.tsx      # 事件处理 — 提交 + 评价反馈
    ├── Evolution.tsx   # 进化引擎 — 种群 + 族谱 + 轨迹
    ├── HowItWorks.tsx  # 原理说明 — 完整技术文档
    ├── Metrics.tsx     # 评估指标 — 仪表盘 + 参数
    ├── Ontology.tsx    # 本体视图 — 概念树
    └── Students.tsx    # 学生数据 — 列表 + 详情
```

## 3. 配色系统

### 3.1 CSS 变量体系

所有颜色通过 `:root` CSS 变量定义在 `index.css` 中，页面组件不应使用硬编码颜色值。

```css
/* 主色 */
--primary: #395141;       /* 深森林绿 — 按钮、链接、激活态 */
--primary-light: #4C6153; /* 苔藓绿 — hover 态 */
--primary-dark: #2F4436;  /* 深墨绿 — 标题、强调 */
--primary-muted: #708070; /* 灰绿色 — 次要信息 */

/* 语义色 — 工作流四阶段 */
--sense: #395141;         /* 感知 — Sense */
--decide: #AC7C5D;        /* 决策 — Decide (陶土棕) */
--act: #93A591;           /* 执行 — Act (灰绿) */
--learn: #D2D1C7;         /* 学习 — Learn (沙色) */
--evolve: #4C6153;        /* 进化 — Evolve (深苔藓绿) */

/* 背景 */
--bg: #F7F6F2;            /* 暖白 — 主背景 */
--bg-card: #FAFAF8;       /* 米白 — 卡片背景 */

/* 文字层次 */
--text: #2F312E;          /* 深灰黑 — 标题/正文 */
--text-secondary: #5F655F;/* 中灰 — 辅助文字 */
--text-muted: #8B908A;    /* 浅灰 — 禁用/弱化 */

/* 强调色 */
--accent: #C48A5A;        /* 琥珀棕 — Evolution 高亮 */
--accent-alt: #B66A50;    /* 赤陶色 — Mutation/Emergence */
```

### 3.2 分类颜色映射

语义类别与本体概念层次的颜色编码：

| 类别 | 颜色 | HEX | 用途 |
|------|------|-----|------|
| 行为问题 (Behavioral) | 赤陶色 | `#B66A50` | 行为类别标签、统计图表 |
| 学业问题 (Academic) | 深森林绿 | `#395141` | 学业类别标签、统计图表 |
| 社交问题 (Social) | 灰绿色 | `#708070` | 社交类别标签、统计图表 |

### 3.3 进化操作颜色

| 操作 | 颜色 | HEX |
|------|------|-----|
| LLM_GENERATE | 浅紫 | `#818cf8` |
| CROSSOVER | 青蓝 | `#06b6d4` |
| PERTURB | 琥珀 | `#f59e0b` |
| MIGRATE | 灰绿 | `#708070` |

## 4. 组件设计规范

### 4.1 页面布局

- 侧栏固定 `260px`，主内容区 `margin-left: 260px`
- 响应式：小屏侧栏收缩至 `60px`（仅图标）
- 页面标题使用 `page-header` 类

### 4.2 卡片 (Card)

```tsx
<div className="card">
  <div className="card-header">
    <div className="card-title">标题</div>
  </div>
  {/* 内容 */}
</div>
```

### 4.3 统计卡片 (Stat Card)

```tsx
<div className="stat-card">
  <div className="stat-label">图标 标签</div>
  <div className="stat-value">数值</div>
  <div className="stat-desc">描述</div>
</div>
```

网格布局：`grid grid-2` / `grid-3` / `grid-4`

### 4.4 空状态处理

所有列表/表格必须处理空数据状态：

```tsx
{data.length === 0 ? (
  <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>
    <p>暂无数据</p>
    <p style={{ fontSize: 13 }}>引导用户操作的提示</p>
  </div>
) : (
  /* 数据表格/列表 */
)}
```

### 4.5 加载状态

```tsx
if (loading) return <div className="loading">加载中...</div>;
```

### 4.6 错误处理

API 调用不应静默吞错误：

```tsx
api.getSomething()
  .then(setData)
  .catch(() => setError('错误信息'))
```

Dashboard 级别的错误展示卡片式错误提示（含重试引导）。

### 4.7 管道图 (Pipeline)

用于展示事件处理/进化流程：

```tsx
<div className="pipeline">
  <div className="pipeline-step active">
    <div className="step-icon">📥</div>
    <div className="step-label">阶段名</div>
  </div>
  <div className="pipeline-arrow">→</div>
  {/* ... */}
</div>
```

### 4.8 评分滑块 (Range Slider)

评价反馈表单使用原生 `<input type="range">`：

```tsx
<input type="range" min="0" max="1" step="0.05"
  value={value}
  onChange={e => setValue(parseFloat(e.target.value))}
  style={{ width: '100%', accentColor: 'var(--primary)' }}
/>
```

## 5. API 调用规范

### 5.1 API 客户端 (`api.ts`)

- 所有后端调用集中在 `api.ts`
- 基础路径：`/api`
- GET 请求使用 `fetchJSON<T>(url)`
- POST 请求使用 `postJSON<T>(url, body)`

### 5.2 可用端点

| 方法 | 端点 | 用途 |
|------|------|------|
| GET | `/api/students` | 学生列表 |
| GET | `/api/students/{id}` | 学生详情 |
| GET | `/api/education/events` | 事件历史 |
| POST | `/api/education/event` | 提交事件 |
| POST | `/api/education/evaluation` | 提交评价 |
| GET | `/api/education/populations` | 种群概览 |
| GET | `/api/education/populations/{iri}` | 种群详情 |
| POST | `/api/education/evolve?conceptIri=` | 触发进化 |
| GET | `/api/education/traces` | 进化轨迹 |
| GET | `/api/education/metrics` | 系统指标 |
| GET | `/api/education/ontology` | 本体数据 |

## 6. 页面职责

| 路由 | 页面 | 数据来源 | 交互 |
|------|------|----------|------|
| `/` | Dashboard | `getMetrics()` | 只读展示 |
| `/students` | Students | `getStudents()` | 搜索 + 详情选择 |
| `/ontology` | Ontology | `getOntology()` | 树展开/折叠 |
| `/events` | Events | `getEvents()` + `processEvent()` + `submitEvaluation()` | 表单 + 评价 |
| `/evolution` | Evolution | `getPopulations()` + `getTraces()` + `triggerEvolution()` | 触发进化 + 族谱切换 |
| `/metrics` | Metrics | `getMetrics()` | 5 秒自动刷新 |
| `/how-it-works` | HowItWorks | 无 API 调用 | 纯静态文档 |

## 7. 进化族谱实现

族谱视图 (`Evolution.tsx`) 使用时间线组件 `GenealogyTree`：

1. 只展示变异操作（LLM_GENERATE / CROSSOVER / PERTURB / MIGRATE）
2. 每条记录显示操作类型徽标、产生的决策名称、亲本来源
3. 时间线连接线通过 CSS `::before` 伪元素实现
4. 颜色编码：生成=紫、交叉=青、微扰=琥珀、迁移=灰绿

## 8. 代码约定

- 使用函数式组件 + Hooks
- 状态管理使用 `useState` + `useEffect`（无全局状态库）
- 无公共 CSS 类名冲突风险 — 使用语义化类名体系
- 在组件内部避免注释，保持代码自文档化
- emoji 仅用于图标占位，非功能性装饰
