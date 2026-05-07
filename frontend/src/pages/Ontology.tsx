import { useEffect, useState } from 'react';
import { api } from '../api';
import type { OntologyNode } from '../types';

function TreeNode({ node, depth = 0 }: { node: OntologyNode; depth?: number }) {
  const [expanded, setExpanded] = useState(true);
  const hasChildren = node.children && node.children.length > 0;

  const categoryColor = (cat: string) => {
    switch (cat) {
      case 'Behavioral': return 'var(--danger)';
      case 'Academic': return '#3b82f6';
      case 'Social': return 'var(--success)';
      default: return 'var(--text-secondary)';
    }
  };

  const catLabel = (cat: string) => {
    switch (cat) {
      case 'Behavioral': return '行为';
      case 'Academic': return '学业';
      case 'Social': return '社交';
      default: return cat;
    }
  };

  return (
    <li>
      <div className="ontology-node" onClick={() => setExpanded(!expanded)} style={{ paddingLeft: depth * 20 + 12 }}>
        {hasChildren && <span style={{ fontSize: 10 }}>{expanded ? '▼' : '▶'}</span>}
        <span className="node-dot" style={{ background: categoryColor(node.category) }} />
        <span style={{ fontWeight: hasChildren ? 600 : 400 }}>{node.label}</span>
        {node.category && (
          <span className="badge" style={{ background: `${categoryColor(node.category)}20`, color: categoryColor(node.category), fontSize: 10 }}>
            {catLabel(node.category)}
          </span>
        )}
        {node.comment && (
          <span style={{ color: 'var(--text-muted)', fontSize: 12, marginLeft: 8 }}>{node.comment}</span>
        )}
      </div>
      {hasChildren && expanded && (
        <ul className="ontology-children">
          {node.children.map((child, i) => <TreeNode key={i} node={child} depth={depth + 1} />)}
        </ul>
      )}
    </li>
  );
}

export default function Ontology() {
  const [tree, setTree] = useState<OntologyNode[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.getOntology()
      .then(data => setTree(Array.isArray(data) ? data : [data]))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="loading">加载本体数据...</div>;

  return (
    <div>
      <div className="page-header">
        <h2>🌳 本体视图</h2>
        <p>教育领域 OWL 本体 — 行为分类层次结构（education.ttl）</p>
      </div>

      <div className="grid grid-2">
        <div className="card">
          <div className="card-header">
            <div className="card-title">概念层次树</div>
          </div>
          {tree.length === 0 ? (
            <div style={{ color: 'var(--text-muted)', fontSize: 14 }}>暂无本体数据</div>
          ) : (
            <ul className="ontology-tree">
              {tree.map((node, i) => <TreeNode key={i} node={node} />)}
            </ul>
          )}
        </div>

        <div>
          <div className="card mb-4">
            <div className="card-header">
              <div className="card-title">本体说明</div>
            </div>
            <div style={{ fontSize: 14, color: 'var(--text-secondary)', lineHeight: 1.8 }}>
              <p>
                本系统使用 <strong>OWL 本体</strong>（Web Ontology Language）定义教育领域的
                行为分类体系。本体文件位于 <code>education.ttl</code>，采用 Turtle 序列化格式。
              </p>
              <div className="explain-box mt-4">
                <p>
                  <strong>推理支持：</strong>当系统启用 RDFS 推理时，
                  子概念会自动继承父概念的属性和干预方案关联。
                  例如 <code>ClassroomDisruption</code> 会自动继承
                  <code>Behavioral</code> 的推荐方案。
                </p>
              </div>
              <div className="code-block" style={{ marginTop: 12, fontSize: 12 }}>
{`@prefix edu: <http://ontoevolve/education#> .

edu:Behavioral rdf:type owl:Class ;
    rdfs:label "行为问题" .

edu:ClassroomDisruption rdf:type owl:Class ;
    rdfs:label "课堂扰乱" ;
    rdfs:subClassOf edu:Behavioral .`}
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <div className="card-title">干预方案类型</div>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {[
                { icon: '💬', name: 'TalkIntervention', label: '谈话教育', desc: '一对一谈话教育干预' },
                { icon: '📞', name: 'NoticeIntervention', label: '通知家长', desc: '通知家长并协同教育' },
                { icon: '🎯', name: 'ActivityIntervention', label: '活动引导', desc: '通过课外活动引导行为改善' },
                { icon: '🏆', name: 'RewardIntervention', label: '奖励机制', desc: '正向激励强化良好行为' },
                { icon: '🧠', name: 'CounselingIntervention', label: '心理辅导', desc: '转介学校心理辅导老师' },
                { icon: '📚', name: 'AcademicSupport', label: '学业辅导', desc: '课后补习或学习支持计划' },
              ].map(item => (
                <div key={item.name} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 12px', background: 'var(--bg-card-hover)', borderRadius: 'var(--radius-sm)' }}>
                  <span>{item.icon}</span>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: 13 }}>{item.label}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{item.name} — {item.desc}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
