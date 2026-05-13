import { useEffect, useState, useCallback } from 'react';
import { api } from '../api';
import type { InterventionSummary, LineageData, OntologyNode } from '../types';

export default function Interventions() {
  const [interventions, setInterventions] = useState<InterventionSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Filters
  const [conceptIri, setConceptIri] = useState('');
  const [status, setStatus] = useState('');
  const [interventionType, setInterventionType] = useState('');
  const [sortBy, setSortBy] = useState('effectiveness');
  const [ontology, setOntology] = useState<OntologyNode[]>([]);

  // Lineage panel
  const [lineageIri, setLineageIri] = useState<string | null>(null);
  const [lineageData, setLineageData] = useState<LineageData | null>(null);
  const [lineageLoading, setLineageLoading] = useState(false);

  useEffect(() => { api.getOntology().then(setOntology).catch(() => {}); }, []);
  useEffect(() => { loadInterventions(); }, [conceptIri, status, interventionType, sortBy]);

  const loadInterventions = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getInterventions({ conceptIri: conceptIri || undefined, status: status || undefined, interventionType: interventionType || undefined, sortBy });
      setInterventions(data);
    } catch {
      setError('无法加载方案数据');
    }
    setLoading(false);
  };

  const openLineage = async (iri: string) => {
    setLineageIri(iri);
    setLineageLoading(true);
    try {
      const data = await api.getInterventionLineage(iri);
      setLineageData(data);
    } catch {
      setLineageData(null);
    }
    setLineageLoading(false);
  };

  const flatOntologyOptions = useCallback((nodes: OntologyNode[], depth = 0): { iri: string; label: string }[] => {
    let result: { iri: string; label: string }[] = [];
    for (const n of nodes) {
      result.push({ iri: n.iri, label: (depth > 0 ? '  '.repeat(depth) : '') + n.label + ` (${n.assignmentCount})` });
      if (n.children) result = result.concat(flatOntologyOptions(n.children, depth + 1));
    }
    return result;
  }, []);

  const statusLabel = (s: string) => {
    const map: Record<string, string> = { ACTIVE: '活跃', ELITE: '精英', NEWBORN: '新生', PROBATION: '观察', DEPRECATED: '废弃' };
    return map[s] || s;
  };
  const statusClass = (s: string) => {
    const map: Record<string, string> = { ACTIVE: 'badge-success', ELITE: 'badge-success', NEWBORN: 'badge-primary', PROBATION: 'badge-warning', DEPRECATED: 'badge-danger' };
    return map[s] || '';
  };

  return (
    <div>
      <div className="page-header">
        <h2>方案库</h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginTop: 4 }}>跨生态位浏览所有干预方案，查看方案谱系和进化血缘</p>
      </div>

      {/* Filter Bar */}
      <div className="card mb-4">
        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div style={{ flex: 1, minWidth: 180 }}>
            <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>生态位</label>
            <select value={conceptIri} onChange={e => setConceptIri(e.target.value)} style={inputStyle}>
              <option value="">全部生态位</option>
              {flatOntologyOptions(ontology).map(c => (
                <option key={c.iri} value={c.iri}>{c.label}</option>
              ))}
            </select>
          </div>
          <div style={{ width: 120 }}>
            <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>状态</label>
            <select value={status} onChange={e => setStatus(e.target.value)} style={inputStyle}>
              <option value="">全部</option>
              <option value="ACTIVE">活跃</option>
              <option value="ELITE">精英</option>
              <option value="NEWBORN">新生</option>
              <option value="PROBATION">观察</option>
              <option value="DEPRECATED">废弃</option>
            </select>
          </div>
          <div style={{ width: 120 }}>
            <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>类型</label>
            <select value={interventionType} onChange={e => setInterventionType(e.target.value)} style={inputStyle}>
              <option value="">全部</option>
              <option value="talk">谈话</option>
              <option value="academic">学业</option>
              <option value="punishment">惩罚</option>
              <option value="reward">奖励</option>
              <option value="support">支持</option>
            </select>
          </div>
          <div style={{ width: 140 }}>
            <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>排序</label>
            <select value={sortBy} onChange={e => setSortBy(e.target.value)} style={inputStyle}>
              <option value="effectiveness">效果优先</option>
              <option value="cost">成本优先</option>
              <option value="trials">试验次数</option>
              <option value="generation">代际</option>
            </select>
          </div>
          <button className="btn" onClick={() => { setConceptIri(''); setStatus(''); setInterventionType(''); setSortBy('effectiveness'); }}>
            重置
          </button>
        </div>
      </div>

      {/* Error */}
      {error && <div className="card mb-4" style={{ color: 'var(--danger)', textAlign: 'center', padding: 16 }}>{error}</div>}

      {/* Intervention Cards */}
      {loading ? (
        <div className="loading">加载中...</div>
      ) : interventions.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: 60, color: 'var(--text-muted)' }}>
          <div style={{ fontSize: 48, marginBottom: 12 }}>💡</div>
          <p>暂无方案数据</p>
          <p style={{ fontSize: 13, marginTop: 8 }}>提交事件并触发进化后，系统会自动生成干预方案</p>
        </div>
      ) : (
        <div style={{ display: 'flex', gap: 24 }}>
          {/* Card Grid */}
          <div style={{ flex: 1, minWidth: 0 }}>
            <div className="grid grid-3">
              {interventions.map((inv) => (
                <div key={inv.iri} className="card" style={{ padding: 20, display: 'flex', flexDirection: 'column', gap: 10 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div style={{ fontWeight: 600, fontSize: 15, flex: 1, minWidth: 0 }}>
                      {inv.name.length > 20 ? inv.name.slice(0, 20) + '...' : inv.name}
                    </div>
                    <span className={`badge ${statusClass(inv.status)}`} style={{ fontSize: 10, flexShrink: 0 }}>
                      {statusLabel(inv.status)}
                    </span>
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                    {inv.conceptLabel || inv.conceptIri?.split('#')[1] || '—'}
                    <span style={{ margin: '0 6px' }}>·</span>
                    {inv.interventionType || '通用'}
                  </div>
                  {inv.description && (
                    <div style={{ fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.6, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                      {inv.description}
                    </div>
                  )}
                  {/* Score bars */}
                  {inv.scoreVector && inv.scoreVector.length >= 2 && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                      <ScoreBar label="效果" value={inv.scoreVector[0]} color="var(--primary)" />
                      <ScoreBar label="成本" value={1 - inv.scoreVector[1]} color="var(--decide)" />
                      {inv.scoreVector[2] !== undefined && <ScoreBar label="满意" value={inv.scoreVector[2]} color="var(--act)" />}
                    </div>
                  )}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 12, color: 'var(--text-secondary)' }}>
                    <span>试验 {inv.trials} 次 · Gen {inv.generation}</span>
                  </div>
                  <button className="btn btn-sm" style={{ alignSelf: 'flex-start', fontSize: 12 }}
                    onClick={() => openLineage(inv.iri)}>
                    查看谱系
                  </button>
                </div>
              ))}
            </div>
          </div>

          {/* Lineage Side Panel */}
          {lineageIri && (
            <div style={{ width: 340, flexShrink: 0 }}>
              <div className="card" style={{ position: 'sticky', top: 24 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                  <div className="card-title" style={{ fontSize: 15 }}>方案谱系</div>
                  <button className="btn btn-sm" onClick={() => { setLineageIri(null); setLineageData(null); }}
                    style={{ fontSize: 11, padding: '2px 8px' }}>✕ 关闭</button>
                </div>
                {lineageLoading ? (
                  <div className="loading" style={{ padding: 20 }}>加载中...</div>
                ) : lineageData ? (
                  <LineagePanel data={lineageData} onNodeClick={openLineage} />
                ) : (
                  <div style={{ textAlign: 'center', padding: 20, color: 'var(--text-muted)', fontSize: 13 }}>无法加载谱系数据</div>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function ScoreBar({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 11 }}>
      <span style={{ color: 'var(--text-muted)', width: 24, flexShrink: 0 }}>{label}</span>
      <div style={{ flex: 1, height: 5, background: 'var(--border-light)', borderRadius: 3 }}>
        <div style={{ width: `${Math.round(value * 100)}%`, height: '100%', background: color, borderRadius: 3 }} />
      </div>
      <span style={{ width: 32, textAlign: 'right', color: 'var(--text-secondary)' }}>{(value * 100).toFixed(0)}%</span>
    </div>
  );
}

function LineagePanel({ data, onNodeClick }: { data: LineageData; onNodeClick: (iri: string) => void }) {
  return (
    <div style={{ fontSize: 13 }}>
      {/* Current */}
      <div style={{ textAlign: 'center', marginBottom: 16, padding: '10px 12px', background: 'rgba(57,81,65,0.06)', borderRadius: 8 }}>
        <div style={{ fontWeight: 700, fontSize: 14 }}>{data.name}</div>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'center', marginTop: 4 }}>
          <span className="badge" style={{ fontSize: 10 }}>Gen {data.generation}</span>
          <span className="badge" style={{ fontSize: 10, background: data.status === 'ACTIVE' || data.status === 'ELITE' ? 'var(--success)' : 'var(--text-muted)', color: '#fff' }}>{data.status}</span>
        </div>
      </div>

      {/* Ancestors */}
      <div style={{ marginBottom: 16 }}>
        <div style={{ fontWeight: 600, marginBottom: 8, color: 'var(--text-secondary)', fontSize: 12 }}>↑ 祖先方案 ({data.ancestors?.length || 0})</div>
        {data.ancestors && data.ancestors.length > 0 ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {data.ancestors.map((a, i) => (
              <div key={i} className="lineage-node" onClick={() => onNodeClick(a.iri)}
                style={{ cursor: 'pointer', padding: '8px 10px', borderRadius: 6, border: '1px solid var(--border-light)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 12, background: 'var(--bg-card)', transition: 'border-color 0.15s' }}>
                <span style={{ fontWeight: 500 }}>{a.name.length > 16 ? a.name.slice(0, 16) + '...' : a.name}</span>
                <span style={{ color: 'var(--text-muted)', fontSize: 11 }}>Gen {a.generation}</span>
              </div>
            ))}
          </div>
        ) : (
          <div style={{ fontSize: 11, color: 'var(--text-muted)', padding: '8px 10px' }}>无（根方案，由 LLM 初始生成）</div>
        )}
      </div>

      {/* Mutation Info */}
      {data.producedBy && data.producedBy.length > 0 && (
        <div style={{ marginBottom: 16 }}>
          <div style={{ fontWeight: 600, marginBottom: 8, color: 'var(--text-secondary)', fontSize: 12 }}>产生方式</div>
          {data.producedBy.map((t, i) => (
            <div key={i} style={{ padding: '6px 10px', borderRadius: 6, background: 'var(--bg)', fontSize: 11, marginBottom: 4 }}>
              <span className="badge" style={{ fontSize: 10, marginRight: 6, background: opColor(t.type), color: '#fff' }}>{opLabel(t.type)}</span>
              <span style={{ color: 'var(--text-secondary)' }}>{t.context || ''}</span>
            </div>
          ))}
        </div>
      )}

      {/* Descendants */}
      <div>
        <div style={{ fontWeight: 600, marginBottom: 8, color: 'var(--text-secondary)', fontSize: 12 }}>↓ 后代方案 ({data.descendants?.length || 0})</div>
        {data.descendants && data.descendants.length > 0 ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {data.descendants.map((d, i) => (
              <div key={i} className="lineage-node" onClick={() => onNodeClick(d.iri)}
                style={{ cursor: 'pointer', padding: '8px 10px', borderRadius: 6, border: '1px solid var(--border-light)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 12, background: 'var(--bg-card)', transition: 'border-color 0.15s' }}>
                <span style={{ fontWeight: 500 }}>{d.name.length > 16 ? d.name.slice(0, 16) + '...' : d.name}</span>
                <span style={{ color: 'var(--text-muted)', fontSize: 11 }}>Gen {d.generation}</span>
              </div>
            ))}
          </div>
        ) : (
          <div style={{ fontSize: 11, color: 'var(--text-muted)', padding: '8px 10px' }}>暂无后代</div>
        )}
      </div>
    </div>
  );
}

function opColor(type: string): string {
  const map: Record<string, string> = { LLM_GENERATE: '#818cf8', CROSSOVER: '#06b6d4', PERTURB: '#f59e0b', MIGRATE: '#708070', SEED: '#8B908A' };
  return map[type] || '#999';
}
function opLabel(type: string): string {
  const map: Record<string, string> = { LLM_GENERATE: 'LLM生成', CROSSOVER: '交叉', PERTURB: '微扰', MIGRATE: '迁移', SEED: '初始' };
  return map[type] || type;
}

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '8px 12px', borderRadius: 6,
  border: '1px solid var(--border)', background: 'var(--bg-card)',
  color: 'var(--text)', fontSize: 13,
};
