import { useEffect, useState } from 'react';
import { api } from '../api';
import type { PopulationView, EvolTrace } from '../types';

// Extend EvolTrace for genealogy
interface TraceWithParents extends EvolTrace {
  decisionIri?: string;
  parents?: { name: string; iri: string }[];
}

function GenealogyTree({ traces: allTraces }: { traces: TraceWithParents[] }) {
  // Only show mutation traces (LLM_GENERATE, CROSSOVER, PERTURB, MIGRATE)
  const traces = allTraces.filter(t =>
    ['LLM_GENERATE', 'CROSSOVER', 'PERTURB', 'MIGRATE'].includes(t.type)
  );
  if (traces.length === 0) return null;

  const opColor = (type: string) => {
    switch (type) {
      case 'LLM_GENERATE': return '#818cf8';
      case 'CROSSOVER': return '#06b6d4';
      case 'PERTURB': return '#f59e0b';
      case 'MIGRATE': return '#10b981';
      default: return 'var(--text-muted)';
    }
  };

  const opLabel = (type: string) => {
    switch (type) {
      case 'LLM_GENERATE': return '🤖 生成';
      case 'CROSSOVER': return '🔀 交叉';
      case 'PERTURB': return '🔧 微扰';
      case 'MIGRATE': return '📤 迁移';
      default: return type;
    }
  };

  return (
    <div className="card mt-6">
      <div className="card-header">
        <div className="card-title">🧬 进化族谱</div>
        <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
          共 {traces.length} 次变异操作
        </span>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 0, position: 'relative' }}>
        {traces.slice().reverse().map((t, idx) => {
          const color = opColor(t.type);
          const hasParents = t.parents && t.parents.length > 0;
          return (
            <div key={idx} style={{
              display: 'flex', gap: 16, padding: '12px 0',
              borderBottom: idx < traces.length - 1 ? '1px solid var(--border-light)' : 'none',
              position: 'relative',
            }}>
              {/* Timeline connector */}
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', width: 40, flexShrink: 0 }}>
                <div style={{
                  width: 12, height: 12, borderRadius: '50%', background: color,
                  border: '2px solid var(--bg-card)', boxShadow: `0 0 0 2px ${color}`,
                  zIndex: 1, flexShrink: 0,
                }} />
                {idx < traces.length - 1 && (
                  <div style={{ width: 2, flex: 1, background: 'var(--border-light)', minHeight: 20 }} />
                )}
              </div>

              {/* Content */}
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  <span className="badge" style={{ background: `${color}20`, color }}>
                    {opLabel(t.type)}
                  </span>
                  <span style={{ fontWeight: 600, fontSize: 14 }}>{t.decision || '—'}</span>
                  <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                    {t.timestamp ? new Date(t.timestamp).toLocaleString('zh-CN') : ''}
                  </span>
                </div>

                {/* Parents */}
                {hasParents && (
                  <div style={{ marginTop: 6, fontSize: 13, color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                    <span>⬆ 源自:</span>
                    {t.parents!.map((p, pi) => (
                      <span key={pi} className="tag" style={{ fontSize: 12 }}>
                        {p.name || p.iri?.split('#')[1] || '—'}
                      </span>
                    ))}
                  </div>
                )}

                {/* Context */}
                {t.context && (
                  <div style={{ marginTop: 4, fontSize: 12, color: 'var(--text-muted)' }}>
                    {(t.context || '').slice(0, 100)}
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default function Evolution() {
  const [populations, setPopulations] = useState<PopulationView[]>([]);
  const [traces, setTraces] = useState<TraceWithParents[]>([]);
  const [loading, setLoading] = useState(true);
  const [evolving, setEvolving] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'populations' | 'genealogy'>('populations');

  const loadData = async () => {
    try {
      const [pops, tr] = await Promise.all([api.getPopulations(), api.getTraces()]);
      setPopulations(pops);
      setTraces(tr);
    } catch (e) {}
    setLoading(false);
  };

  useEffect(() => { loadData(); }, []);

  const triggerEvolve = async (iri: string) => {
    setEvolving(iri);
    try {
      await api.triggerEvolution(iri);
      await loadData();
    } catch (e) {}
    setEvolving(null);
  };

  const categoryColor = (cat: string) => {
    switch (cat) {
      case 'Behavioral': return '#ef4444';
      case 'Academic': return '#3b82f6';
      case 'Social': return '#10b981';
      default: return 'var(--text-secondary)';
    }
  };

  if (loading) return <div className="loading">加载进化数据...</div>;

  return (
    <div>
      <div className="page-header">
        <h2>🧬 进化引擎</h2>
        <p>生态位种群监控 — 查看种群状态、进化族谱和轨迹</p>
      </div>

      <div className="explain-box mb-6">
        <p>
          <strong>进化状态面板</strong> — 每个生态位（概念节点）下维护一个决策方案种群。
          种群通过<strong>变异 → 选择 → 迁移</strong>的进化循环逐代优化。
          颜色标识：<span style={{ color: '#ef4444' }}>🔴 行为问题</span> ·
          <span style={{ color: '#3b82f6' }}>🔵 学业问题</span> ·
          <span style={{ color: '#10b981' }}>🟢 社交问题</span>
        </p>
      </div>

      {/* Tab switch */}
      {populations.length > 0 && traces.length > 0 && (
        <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
          <button
            className={`btn ${activeTab === 'populations' ? 'btn-primary' : ''}`}
            style={{ background: activeTab === 'populations' ? undefined : 'var(--bg-card-hover)', color: activeTab === 'populations' ? undefined : 'var(--text-secondary)' }}
            onClick={() => setActiveTab('populations')}>
            🌿 种群概览
          </button>
          <button
            className={`btn ${activeTab === 'genealogy' ? 'btn-primary' : ''}`}
            style={{ background: activeTab === 'genealogy' ? undefined : 'var(--bg-card-hover)', color: activeTab === 'genealogy' ? undefined : 'var(--text-secondary)' }}
            onClick={() => setActiveTab('genealogy')}>
            🧬 进化族谱
          </button>
        </div>
      )}

      {populations.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>
          <div style={{ fontSize: 48, marginBottom: 12 }}>🌿</div>
          <p>暂无种群数据</p>
          <p style={{ fontSize: 13, marginTop: 8 }}>请先在"事件处理"页面提交行为事件，系统会自动创建种群</p>
        </div>
      ) : activeTab === 'populations' ? (
        <>
          {/* Population Cards */}
          <div className="grid" style={{ gap: 20 }}>
            {populations.map(pop => (
              <div key={pop.conceptIri} className="card">
                <div className="card-header">
                  <div className="flex items-center gap-2">
                    <span className="node-dot" style={{ background: categoryColor(pop.conceptCategory), width: 12, height: 12 }} />
                    <div className="card-title">{pop.conceptLabel}</div>
                    <span className="badge"
                      style={{ background: `${categoryColor(pop.conceptCategory)}20`, color: categoryColor(pop.conceptCategory) }}>
                      {pop.conceptCategory === 'Behavioral' ? '行为' : pop.conceptCategory === 'Academic' ? '学业' : '社交'}
                    </span>
                  </div>
                  <button className="btn btn-sm btn-primary" onClick={() => triggerEvolve(pop.conceptIri)} disabled={evolving === pop.conceptIri}>
                    {evolving === pop.conceptIri ? '进化中...' : '触发进化'}
                  </button>
                </div>

                {/* Population stats bar */}
                <div style={{ display: 'flex', gap: 24, padding: '8px 0', fontSize: 13 }}>
                  <div>
                    <span style={{ color: 'var(--text-muted)' }}>代数</span>
                    <div style={{ fontWeight: 600, fontSize: 18 }}>{pop.generation}</div>
                  </div>
                  <div>
                    <span style={{ color: 'var(--text-muted)' }}>方案数</span>
                    <div style={{ fontWeight: 600, fontSize: 18 }}>{pop.size}</div>
                  </div>
                  <div>
                    <span style={{ color: 'var(--text-muted)' }}>活跃</span>
                    <div style={{ fontWeight: 600, fontSize: 18, color: 'var(--success)' }}>{pop.activeCount}</div>
                  </div>
                </div>

                {/* Progress bar for active ratio */}
                {pop.size > 0 && (
                  <div style={{ marginTop: 4 }}>
                    <div className="progress-bar">
                      <div className="progress-bar-fill" style={{
                        width: `${(pop.activeCount / pop.size) * 100}%`,
                        background: 'var(--success)',
                      }} />
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>

          {/* Traces Table */}
          {traces.length > 0 && (
            <div className="card mt-6">
              <div className="card-header">
                <div className="card-title">进化轨迹</div>
                <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{traces.length} 条记录</span>
              </div>
              <div className="table-container" style={{ maxHeight: 400, overflowY: 'auto' }}>
                <table>
                  <thead>
                    <tr>
                      <th>操作类型</th>
                      <th>产生方案</th>
                      <th>亲本来源</th>
                      <th>时间</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[...traces].reverse().map((t, i) => {
                      const opColors: Record<string, string> = {
                        LLM_GENERATE: 'badge-primary', CROSSOVER: 'badge-info',
                        PERTURB: 'badge-warning', MIGRATE: 'badge-success',
                      };
                      const parentNames = t.parents?.map(p => p.name).filter(Boolean).join(', ') || '—';
                      return (
                        <tr key={i}>
                          <td><span className={`badge ${opColors[t.type] || ''}`}>{t.type}</span></td>
                          <td style={{ fontSize: 13, fontWeight: 500 }}>{t.decision || '—'}</td>
                          <td style={{ fontSize: 12, color: 'var(--text-secondary)', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {parentNames}
                          </td>
                          <td style={{ fontSize: 11, color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                            {t.timestamp ? new Date(t.timestamp).toLocaleString('zh-CN') : '—'}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      ) : (
        /* Genealogy Tab */
        <>
          <GenealogyTree traces={traces} />
          {traces.length > 0 && (
            <div className="card mt-6">
              <div className="card-header">
                <div className="card-title">📋 全量轨迹日志</div>
                <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{traces.length} 条</span>
              </div>
              <div className="table-container" style={{ maxHeight: 500, overflowY: 'auto' }}>
                <table>
                  <thead>
                    <tr>
                      <th>操作</th>
                      <th>方案</th>
                      <th>亲本</th>
                      <th>上下文</th>
                      <th>时间</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[...traces].reverse().map((t, i) => {
                      const opColors: Record<string, string> = {
                        LLM_GENERATE: 'badge-primary', CROSSOVER: 'badge-info',
                        PERTURB: 'badge-warning', MIGRATE: 'badge-success',
                      };
                      return (
                        <tr key={i}>
                          <td><span className={`badge ${opColors[t.type] || ''}`}>{t.type}</span></td>
                          <td style={{ fontSize: 13, fontWeight: 500 }}>{t.decision || '—'}</td>
                          <td style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                            {t.parents?.map(p => p.name).filter(Boolean).join(', ') || '—'}
                          </td>
                          <td style={{ fontSize: 12, color: 'var(--text-secondary)', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {(t.context || '').slice(0, 60)}
                          </td>
                          <td style={{ fontSize: 11, color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                            {t.timestamp ? new Date(t.timestamp).toLocaleString('zh-CN') : '—'}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
