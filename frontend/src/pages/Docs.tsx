import { useEffect, useState } from 'react';
import { api } from '../api';
import type { OntologyNode } from '../types';

const SECTIONS = [
  { id: 'intro', label: '系统介绍' },
  { id: 'manual', label: '用户手册' },
  { id: 'ontology', label: '本体参考' },
  { id: 'changelog', label: '更新日志' },
];

export default function Docs() {
  const [activeSection, setActiveSection] = useState('intro');
  const [ontologyData, setOntologyData] = useState<OntologyNode[]>([]);

  useEffect(() => {
    if (activeSection === 'ontology') {
      api.getOntology().then(setOntologyData).catch(() => {});
    }
  }, [activeSection]);

  return (
    <div>
      <div className="page-header">
        <h2>文档中心</h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginTop: 4 }}>系统介绍 · 用户手册 · 本体参考 · 更新日志</p>
      </div>

      <div style={{ display: 'flex', gap: 24 }}>
        {/* Sidebar */}
        <nav style={{ width: 180, flexShrink: 0 }}>
          <div className="card" style={{ position: 'sticky', top: 24 }}>
            <div style={{ padding: 12 }}>
              {SECTIONS.map(s => (
                <div key={s.id}
                  onClick={() => setActiveSection(s.id)}
                  style={{
                    padding: '8px 12px', borderRadius: 6, cursor: 'pointer', fontSize: 13,
                    background: activeSection === s.id ? 'var(--primary)' : 'transparent',
                    color: activeSection === s.id ? '#fff' : 'var(--text-secondary)',
                    marginBottom: 2,
                  }}>
                  {s.label}
                </div>
              ))}
            </div>
          </div>
        </nav>

        {/* Content */}
        <div style={{ flex: 1, minWidth: 0 }}>
          {activeSection === 'intro' && <IntroSection />}
          {activeSection === 'manual' && <ManualSection />}
          {activeSection === 'ontology' && <OntologySection data={ontologyData} />}
          {activeSection === 'changelog' && <ChangelogSection />}
        </div>
      </div>
    </div>
  );
}

function IntroSection() {
  return (
    <div>
      <div className="card mb-4">
        <div className="card-title" style={{ marginBottom: 16 }}>OntoEvolve 是什么</div>
        <p style={{ color: 'var(--text-secondary)', fontSize: 14, lineHeight: 1.8 }}>
          <strong style={{ color: 'var(--text)' }}>OntoEvolve</strong> 是一个<strong>本体驱动的自进化决策框架</strong>。
          它将语义技术（OWL本体）与进化计算相结合，构建"感知→决策→执行→学习→进化"的闭环决策系统。
        </p>
        <div style={{ background: 'var(--bg)', padding: 16, borderRadius: 8, marginTop: 16 }}>
          <p style={{ fontSize: 14 }}>
            <strong>核心理念：</strong>每个行为问题都是一个"生态位"（Niche），
            每个生态位中维护着一个"决策方案种群"（Population）。
            种群通过进化算法代际迭代，持续优化方案质量。
          </p>
        </div>
      </div>

      <h3 style={{ fontSize: 18, margin: '24px 0 16px' }}>五大核心环节</h3>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <StepCard num={1} title="感知 Sense — 事件分类" color="var(--sense)">
          系统接收学生行为事件，通过 LLM 分类器将事件映射到本体概念类别（如"课堂扰乱"、"作业不交"），
          支持层次回退：子类别无匹配时回退到父类别。
        </StepCard>
        <StepCard num={2} title="决策 Decide — 方案匹配" color="var(--decide)">
          在对应生态位的方案种群中，使用 Pareto + UCB 混合策略匹配最优方案。
          同时返回种群中所有候选方案供用户选择。
        </StepCard>
        <StepCard num={3} title="执行 Act — 实施干预" color="var(--act)">
          教师执行选定的干预方案，系统记录执行上下文（执行者、时间、目标学生）。
        </StepCard>
        <StepCard num={4} title="学习 Learn — 效果评价" color="var(--learn)">
          对干预效果进行三维评价（有效性、成本、满意度），使用 Welford 在线算法更新方案评分统计。
          信用分配机制将延迟反馈传播到历史执行。
        </StepCard>
        <StepCard num={5} title="进化 Evolve — 种群演化" color="var(--evolve)">
          当累积反馈达到阈值，触发进化循环：变异（LLM生成/交叉/微扰）→ 选择（Pareto支配+拥挤距离）→
          迁移（跨生态位知识复用）。元优化器自适应调整进化参数。
        </StepCard>
      </div>

      <div className="card mt-4">
        <div className="card-title" style={{ marginBottom: 12 }}>技术架构</div>
        <div style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.8 }}>
          <p><strong>语言：</strong>Java 17 + Spring Boot 3.2</p>
          <p><strong>本体推理：</strong>Apache Jena 5.0（OWL加载、InfModel推理）</p>
          <p><strong>LLM集成：</strong>Spring AI + DeepSeek（分类与变异生成）</p>
          <p><strong>图数据库：</strong>Neo4j 5（种群、方案、事件-反馈图谱持久化）</p>
          <p><strong>前端：</strong>React 18 + TypeScript + Vite</p>
        </div>
      </div>
    </div>
  );
}

function StepCard({ num, title, color, children }: { num: number; title: string; color: string; children: string }) {
  return (
    <div className="card" style={{ borderLeft: `4px solid ${color}` }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
        <span style={{
          background: color, color: '#fff', borderRadius: '50%',
          width: 28, height: 28, display: 'flex', alignItems: 'center',
          justifyContent: 'center', fontWeight: 700, fontSize: 14, flexShrink: 0
        }}>{num}</span>
        <span style={{ fontWeight: 600, fontSize: 15 }}>{title}</span>
      </div>
      <p style={{ color: 'var(--text-secondary)', fontSize: 13, lineHeight: 1.8 }}>{children}</p>
    </div>
  );
}

function ManualSection() {
  return (
    <div>
      <h3 style={{ fontSize: 18, marginBottom: 16 }}>用户操作手册</h3>

      <div className="card mb-4">
        <div className="card-title" style={{ marginBottom: 12 }}>工作台</div>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.8 }}>
          系统首页展示全局运行概览：今日事件统计、待评价数量、活跃方案数、进化代次等关键指标。
          最近活动时间线显示最新提交的行为事件及其处理结果。方案效果排行帮助快速了解哪些干预措施最有效。
          生态位健康概览展示每个概念类别下方案种群的状态。
        </p>
      </div>

      <div className="card mb-4">
        <div className="card-title" style={{ marginBottom: 12 }}>事件追溯</div>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.8 }}>
          浏览所有已提交的行为事件，支持按类别、严重程度、学生筛选。
          点击事件行可展开查看完整处理链路：事件详情 → LLM分类结果 → 匹配的干预方案 → 执行记录 → 效果评价。
          可以通过"新建事件"按钮提交新的行为事件，系统会自动分类并匹配最优方案。
          对已执行的方案，可以提交三维效果评价（有效性、成本、满意度）。
        </p>
      </div>

      <div className="card mb-4">
        <div className="card-title" style={{ marginBottom: 12 }}>方案库</div>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.8 }}>
          跨生态位浏览所有干预方案。每个方案卡片展示名称、描述、执行步骤、三维评分、试验次数、代际、状态。
          支持按生态位、状态、方案类型筛选，按效果/试验次数/代际排序。
          点击"查看谱系"可追溯方案的进化血缘：哪些方案是它的祖先（通过交叉或变异产生），它又生成了哪些后代方案。
        </p>
      </div>

      <div className="card mb-4">
        <div className="card-title" style={{ marginBottom: 12 }}>关系图谱</div>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.8 }}>
          左侧展示本体概念层次树，每个节点后的数字表示该概念下关联的方案数和事件数。
          点击某个概念后，右侧展示该概念下的实例关系图：事件节点 → 概念节点 → 方案节点 → 干预节点，
          以及它们之间的分类、匹配、谱系等关系连线。点击图中节点可查看详情。
        </p>
      </div>

      <div className="card mb-4">
        <div className="card-title" style={{ marginBottom: 12 }}>进化监控</div>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.8 }}>
          监控每个生态位的方案种群进化状态。种群概览展示各生态位的代际、方案数、精英数。
          点击"触发进化"可手动启动一次完整进化循环（变异+选择+迁移）。
          Pareto前沿散点图展示方案在"效果-成本"空间中的分布，前沿上的方案互不支配。
          进化轨迹时间线记录每次变异操作的元数据，支持谱系追溯。
        </p>
      </div>
    </div>
  );
}

function OntologySection({ data }: { data: OntologyNode[] }) {
  return (
    <div>
      <h3 style={{ fontSize: 18, marginBottom: 16 }}>本体概念参考</h3>
      <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 16 }}>
        以下是从 education.ttl 加载的本体概念层次。括号内数字为关联的方案数/事件数。
      </p>
      {data.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>
          本体数据加载中...
        </div>
      ) : (
        data.map(root => <OntologyTreeNode key={root.iri} node={root} depth={0} />)
      )}

      <h3 style={{ fontSize: 18, margin: '24px 0 16px' }}>评分维度说明</h3>
      <div className="card">
        <table>
          <thead>
            <tr><th>维度</th><th>含义</th><th>取值范围</th></tr>
          </thead>
          <tbody>
            <tr><td>effectiveness（有效性）</td><td>方案对改善行为的实际效果</td><td>0.0 ~ 1.0</td></tr>
            <tr><td>cost（成本）</td><td>执行方案所需的时间、人力成本</td><td>0.0 ~ 1.0（越低越好）</td></tr>
            <tr><td>satisfaction（满意度）</td><td>学生/教师对方案的接受度</td><td>0.0 ~ 1.0</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}

function OntologyTreeNode({ node, depth }: { node: OntologyNode; depth: number }) {
  const [expanded, setExpanded] = useState(depth < 2);
  const hasChildren = node.children && node.children.length > 0;
  const catColor = node.category === 'Behavioral' ? '#B66A50' :
    node.category === 'Academic' ? '#395141' : '#708070';

  return (
    <div style={{ marginLeft: depth * 20 }}>
      <div
        onClick={() => hasChildren && setExpanded(!expanded)}
        style={{
          display: 'flex', alignItems: 'center', gap: 8, padding: '6px 10px',
          borderRadius: 6, cursor: hasChildren ? 'pointer' : 'default',
          fontSize: 13, marginBottom: 2,
        }}>
        {hasChildren ? <span style={{ fontSize: 10 }}>{expanded ? '▼' : '▶'}</span> : <span style={{ width: 16 }} />}
        <span style={{
          width: 10, height: 10, borderRadius: '50%', background: catColor, flexShrink: 0
        }} />
        <span style={{ fontWeight: depth < 2 ? 600 : 400 }}>{node.label}</span>
        <span style={{ color: 'var(--text-muted)', fontSize: 11 }}>
          ({node.assignmentCount}方案/{node.eventCount}事件)
        </span>
      </div>
      {expanded && hasChildren && node.children.map(child => (
        <OntologyTreeNode key={child.iri} node={child} depth={depth + 1} />
      ))}
    </div>
  );
}

function ChangelogSection() {
  const versions = [
    {
      version: 'v0.2.0',
      date: '2026-05',
      changes: [
        '产品化重构：导航精简为6项（工作台/事件追溯/方案库/关系图谱/进化监控/文档中心）',
        '新增工作台仪表盘：实时活动时间线、方案效果排行、生态位健康概览',
        '新增事件追溯模块：分页筛选、完整处理链路展开',
        '新增方案库模块：跨生态位方案浏览、谱系追溯、筛选排序',
        '新增关系图谱模块：本体树 + 实例关系力导向图',
        '进化监控增强：Pareto前沿散点图、变异操作统计',
        '文档中心：系统介绍、用户手册、本体参考、更新日志',
        '后端新增6个API端点，增强3个已有端点',
      ],
    },
    {
      version: 'v0.1.0',
      date: '2026-04',
      changes: [
        '初始版本，完成核心闭环：事件→分类→匹配→执行→评价→进化',
        'LLM驱动行为分类器（DeepSeek）',
        '三种变异算子：LLM生成、交叉重组、参数微扰',
        'Pareto支配 + 拥挤距离多目标选择',
        '跨生态位语义迁移',
        '元优化器自适应参数调整',
        'Neo4j图存储 + 内存双模式',
        'React前端（7页面）',
        '教育领域OWL本体（education.ttl）',
      ],
    },
  ];

  return (
    <div>
      <h3 style={{ fontSize: 18, marginBottom: 16 }}>更新日志</h3>
      {versions.map(v => (
        <div key={v.version} className="card mb-4">
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, marginBottom: 12 }}>
            <span style={{ fontWeight: 700, fontSize: 16, color: 'var(--primary)' }}>{v.version}</span>
            <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>{v.date}</span>
          </div>
          <ul style={{ margin: 0, paddingLeft: 20, fontSize: 13, color: 'var(--text-secondary)', lineHeight: 2 }}>
            {v.changes.map((c, i) => <li key={i}>{c}</li>)}
          </ul>
        </div>
      ))}
    </div>
  );
}
