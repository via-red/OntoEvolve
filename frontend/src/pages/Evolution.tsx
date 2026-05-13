import { useEffect, useState, useMemo, useCallback, useRef } from 'react';
import { api } from '../api';
import Tree from 'react-d3-tree';
import { ScatterChart, Scatter, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar, Cell } from 'recharts';
import type { RawNodeDatum, CustomNodeElementProps, Point } from 'react-d3-tree';
import type { PopulationView, AssignmentView, EvolTrace, PopulationDetail } from '../types';

const OP_COLORS: Record<string, string> = {
  LLM_GENERATE: '#818cf8',
  CROSSOVER: '#06b6d4',
  PERTURB: '#f59e0b',
  MIGRATE: '#708070',
  SEED: '#8B908A',
};

const OP_LABELS: Record<string, string> = {
  LLM_GENERATE: '生成',
  CROSSOVER: '交叉',
  PERTURB: '微扰',
  MIGRATE: '迁移',
  SEED: '初始',
};

const CATEGORY_COLORS: Record<string, string> = {
  Behavioral: '#B66A50',
  Academic: '#395141',
  Social: '#708070',
};

/* ───── Ancestry Tree Component (react-d3-tree) ───── */

function AncestryTree({ decisionIri, traces }: { decisionIri: string; traces: EvolTrace[] }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const treeScope = useRef(`gt-${Math.random().toString(36).slice(2, 8)}`).current;
  const [dimensions, setDimensions] = useState({ width: 600, height: 400 });

  // Build node lookup
  const nodeMap = useMemo(() => {
    const map = new Map<string, { name: string; type: string; parents: { name: string; iri: string }[] }>();
    traces.forEach(t => {
      const iri = t.decisionIri;
      if (iri) {
        map.set(iri, { name: t.decision, type: t.type, parents: t.parents || [] });
      }
      (t.parents || []).forEach(p => {
        if (!map.has(p.iri)) {
          map.set(p.iri, { name: p.name, type: 'SEED', parents: [] });
        }
      });
    });
    return map;
  }, [traces]);

  // Build RawNodeDatum tree: root = selected (renders at bottom with depthFactor < 0)
  const buildTreeData = useCallback((iri: string): RawNodeDatum | null => {
    const info = nodeMap.get(iri);
    if (!info) return null;
    const children = info.parents
      .map(p => buildTreeData(p.iri))
      .filter((n): n is RawNodeDatum => n !== null);
    return {
      name: info.name,
      attributes: { type: info.type },
      children: children.length > 0 ? children : undefined,
    };
  }, [nodeMap]);

  const treeData = useMemo(() => decisionIri ? buildTreeData(decisionIri) : null, [decisionIri, buildTreeData]);

  // Measure container
  useEffect(() => {
    if (containerRef.current) {
      const rect = containerRef.current.getBoundingClientRect();
      setDimensions({ width: rect.width || 600, height: 450 });
    }
  }, [treeData]);

  const translate: Point = { x: dimensions.width / 2, y: dimensions.height - 60 };

  // Genealogy-style node card with solid background
  const renderNode = useCallback(({ nodeDatum }: CustomNodeElementProps) => {
    const type = (nodeDatum.attributes?.type as string) || 'SEED';
    const c = OP_COLORS[type] || '#999';
    return (
      <foreignObject width="160" height="52" x="-80" y="-26" style={{ overflow: 'visible' }}>
        <div style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
          width: 160, height: 52, boxSizing: 'border-box',
          padding: '6px 14px', gap: 2,
          borderRadius: 8,
          background: 'var(--bg-card)',
          border: `2px solid ${c}`,
          boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
          whiteSpace: 'nowrap', fontFamily: 'inherit',
          cursor: 'default',
        }}>
          <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--text)', lineHeight: 1.3, maxWidth: 130, overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {nodeDatum.name.length > 18 ? nodeDatum.name.slice(0, 18) + '…' : nodeDatum.name}
          </span>
          <span style={{ fontSize: 10, color: c, fontWeight: 500, lineHeight: 1.4 }}>
            ● {OP_LABELS[type] || type}
          </span>
        </div>
      </foreignObject>
    );
  }, []);

  if (!treeData) {
    return <div style={{ padding: 20, color: 'var(--text-muted)', textAlign: 'center' }}>未找到族谱数据</div>;
  }

  return (
    <div className="card mt-4">
      <div className="card-header">
        <div className="card-title">🧬 进化族谱</div>
      </div>
      <div className={treeScope} ref={containerRef} style={{ width: '100%', height: 450 }}>
        <style>{`
          .${treeScope} .rd3t-link {
            fill: none;
            stroke: #b0b8c0;
            stroke-width: 1.5;
            opacity: 0.6;
          }
        `}</style>
        <Tree
          data={treeData}
          orientation="vertical"
          depthFactor={-200}
          translate={translate}
          nodeSize={{ x: 200, y: 90 }}
          separation={{ siblings: 1.2, nonSiblings: 1.8 }}
          pathFunc="diagonal"
          collapsible={false}
          zoomable={true}
          draggable={true}
          renderCustomNodeElement={renderNode}
        />
      </div>
    </div>
  );
}

/* ───── Main Evolution Page ───── */

export default function Evolution() {
  const [populations, setPopulations] = useState<PopulationView[]>([]);
  const [traces, setTraces] = useState<EvolTrace[]>([]);
  const [loading, setLoading] = useState(true);
  const [evolving, setEvolving] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'populations' | 'genealogy'>('populations');

  // Population drill-down: conceptIri → members + pareto
  const [expandedPop, setExpandedPop] = useState<string | null>(null);
  const [popMembers, setPopMembers] = useState<Record<string, AssignmentView[]>>({});
  const [popDetails, setPopDetails] = useState<Record<string, PopulationDetail>>({});
  const [membersLoading, setMembersLoading] = useState(false);

  // Genealogy selection
  const [selectedDecisionIri, setSelectedDecisionIri] = useState<string | null>(null);

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

  const togglePopulation = async (conceptIri: string) => {
    if (expandedPop === conceptIri) {
      setExpandedPop(null);
      return;
    }
    setExpandedPop(conceptIri);
    if (!popMembers[conceptIri]) {
      setMembersLoading(true);
      try {
        const detail = await api.getPopulation(conceptIri);
        setPopMembers(prev => ({ ...prev, [conceptIri]: detail.members || [] }));
        setPopDetails(prev => ({ ...prev, [conceptIri]: detail }));
      } catch (e) {}
      setMembersLoading(false);
    }
  };

  // Variator stats from traces
  const variatorStats = useMemo(() => {
    const counts: Record<string, number> = {};
    traces.forEach(t => {
      counts[t.type] = (counts[t.type] || 0) + 1;
    });
    return Object.entries(counts).map(([type, count]) => ({ type, count }));
  }, [traces]);

  const viewGenealogy = (decisionIri: string) => {
    setSelectedDecisionIri(decisionIri);
    setActiveTab('genealogy');
  };

  // Collect all unique decision names for the genealogy selector
  const allDecisions = useMemo(() => {
    const seen = new Set<string>();
    return traces
      .filter(t => t.decisionIri && t.decision)
      .filter(t => {
        if (seen.has(t.decisionIri!)) return false;
        seen.add(t.decisionIri!);
        return true;
      })
      .map(t => ({ iri: t.decisionIri!, name: t.decision }));
  }, [traces]);

  if (loading) return <div className="loading">加载进化数据...</div>;

  return (
    <div>
      <div className="page-header">
        <h2>🧬 进化引擎</h2>
        <p>生态位种群监控 — 点击种群查看方案列表，点击方案查看进化族谱</p>
      </div>

      {/* Tab switch */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        <button
          className={`btn ${activeTab === 'populations' ? 'btn-primary' : ''}`}
          style={{
            background: activeTab === 'populations' ? undefined : 'var(--bg-card-hover)',
            color: activeTab === 'populations' ? undefined : 'var(--text-secondary)',
          }}
          onClick={() => setActiveTab('populations')}>
          🌿 种群概览
        </button>
        <button
          className={`btn ${activeTab === 'genealogy' ? 'btn-primary' : ''}`}
          style={{
            background: activeTab === 'genealogy' ? undefined : 'var(--bg-card-hover)',
            color: activeTab === 'genealogy' ? undefined : 'var(--text-secondary)',
          }}
          onClick={() => setActiveTab('genealogy')}>
          🧬 进化族谱
        </button>
      </div>

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
            {populations.map(pop => {
              const catColor = CATEGORY_COLORS[pop.conceptCategory] || 'var(--text-secondary)';
              const isExpanded = expandedPop === pop.conceptIri;
              return (
                <div key={pop.conceptIri} className="card" style={{ cursor: 'pointer' }}>
                  <div className="card-header" onClick={() => togglePopulation(pop.conceptIri)}>
                    <div className="flex items-center gap-2">
                      <span className="node-dot" style={{ background: catColor, width: 12, height: 12 }} />
                      <div className="card-title">{pop.conceptLabel || pop.conceptIri.split('#')[1]}</div>
                      <span className="badge" style={{ background: `${catColor}20`, color: catColor }}>
                        {pop.conceptCategory === 'Behavioral' ? '行为' : pop.conceptCategory === 'Academic' ? '学业' : '社交'}
                      </span>
                    </div>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <button className="btn btn-sm btn-primary"
                        onClick={e => { e.stopPropagation(); triggerEvolve(pop.conceptIri); }}
                        disabled={evolving === pop.conceptIri}>
                        {evolving === pop.conceptIri ? '进化中...' : '触发进化'}
                      </button>
                      <span style={{ color: 'var(--text-muted)', fontSize: 12, alignSelf: 'center' }}>
                        {isExpanded ? '▲ 收起' : '▼ 展开'}
                      </span>
                    </div>
                  </div>

                  {/* Stats bar */}
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

                  {/* Expanded members list */}
                  {isExpanded && (
                    <div style={{ marginTop: 12, borderTop: '1px solid var(--border-light)', paddingTop: 12 }}>
                      <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8, color: 'var(--text-secondary)' }}>
                        方案列表 ({popMembers[pop.conceptIri]?.length || 0})
                      </div>
                      {membersLoading ? (
                        <div className="loading" style={{ padding: 12 }}>加载中...</div>
                      ) : !popMembers[pop.conceptIri] || popMembers[pop.conceptIri].length === 0 ? (
                        <div style={{ padding: 12, color: 'var(--text-muted)', fontSize: 13 }}>
                          暂无方案数据，请先触发进化
                        </div>
                      ) : (
                        <div className="table-container" style={{ maxHeight: 300, overflowY: 'auto' }}>
                          <table>
                            <thead>
                              <tr>
                                <th>方案名称</th>
                                <th>评分</th>
                                <th>尝试</th>
                                <th>代数</th>
                                <th>状态</th>
                                <th>操作</th>
                              </tr>
                            </thead>
                            <tbody>
                              {popMembers[pop.conceptIri].map((m, i) => (
                                <tr key={i}>
                                  <td style={{ fontWeight: 500, fontSize: 13 }}>{m.name}</td>
                                  <td style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                                    {m.scoreVector ? `[${m.scoreVector.map(s => s.toFixed(2)).join(', ')}]` : '—'}
                                  </td>
                                  <td style={{ fontSize: 12 }}>{m.trials}</td>
                                  <td style={{ fontSize: 12 }}>{m.generation}</td>
                                  <td>
                                    <span className={`badge ${statusClass(m.status)}`} style={{ fontSize: 11 }}>
                                      {statusLabel(m.status)}
                                    </span>
                                    {m.validationStatus && (
                                      <span className="badge" style={{
                                        fontSize: 11, marginLeft: 4,
                                        background: m.validationStatus === 'valid' ? 'var(--success)' : '#f59e0b',
                                        color: '#fff',
                                      }}>
                                        {m.validationStatus === 'valid' ? '✓' : '?'}
                                      </span>
                                    )}
                                  </td>
                                  <td>
                                    <button className="btn btn-sm"
                                      style={{ fontSize: 12 }}
                                      onClick={() => viewGenealogy(m.decisionIri)}>
                                      查看族谱
                                    </button>
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}

                      {/* Pareto Scatter Chart */}
                      {popDetails[pop.conceptIri]?.paretoPoints && popDetails[pop.conceptIri].paretoPoints!.length > 0 && (
                        <div style={{ marginTop: 16, borderTop: '1px solid var(--border-light)', paddingTop: 16 }}>
                          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8, color: 'var(--text-secondary)' }}>
                            Pareto 前沿 (效果 vs 成本)
                          </div>
                          <ResponsiveContainer width="100%" height={200}>
                            <ScatterChart margin={{ top: 10, right: 20, bottom: 10, left: 0 }}>
                              <CartesianGrid strokeDasharray="3 3" stroke="var(--border-light)" />
                              <XAxis dataKey="effectiveness" name="效果" unit="" domain={[0, 1]} tick={{ fontSize: 11 }} stroke="var(--text-muted)" />
                              <YAxis dataKey="cost" name="成本" unit="" domain={[0, 1]} tick={{ fontSize: 11 }} stroke="var(--text-muted)" />
                              <Tooltip cursor={{ strokeDasharray: '3 3' }}
                                contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 6, fontSize: 12 }} />
                              <Scatter data={popDetails[pop.conceptIri].paretoPoints} fill="var(--primary)" />
                            </ScatterChart>
                          </ResponsiveContainer>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {/* Variator Stats + Pareto Summary */}
          <div className="grid grid-2 mt-6">
            {/* Variator Stats Bar Chart */}
            {variatorStats.length > 0 && (
              <div className="card">
                <div className="card-header">
                  <div className="card-title">变异算子统计</div>
                </div>
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={variatorStats} margin={{ top: 10, right: 20, bottom: 10, left: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border-light)" />
                    <XAxis dataKey="type" tick={{ fontSize: 11 }} stroke="var(--text-muted)" />
                    <YAxis tick={{ fontSize: 11 }} stroke="var(--text-muted)" allowDecimals={false} />
                    <Tooltip contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 6, fontSize: 12 }} />
                    <Bar dataKey="count" name="次数">
                      {variatorStats.map((entry, index) => (
                        <Cell key={index} fill={OP_COLORS[entry.type] || '#999'} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}

            {/* Population Scores Overview */}
            {populations.length > 0 && (
              <div className="card">
                <div className="card-header">
                  <div className="card-title">种群平均效果概览</div>
                </div>
                <div style={{ maxHeight: 240, overflowY: 'auto' }}>
                  {populations.map(pop => (
                    <div key={pop.conceptIri} style={{ padding: '8px 0', borderBottom: '1px solid var(--border-light)' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4, fontSize: 12 }}>
                        <span>{pop.conceptLabel}</span>
                        <span style={{ color: 'var(--text-muted)' }}>Gen {pop.generation} · {pop.size}方案</span>
                      </div>
                      <div style={{ display: 'flex', gap: 16, fontSize: 11, color: 'var(--text-secondary)' }}>
                        <span>效果 {(pop.avgEffectiveness || 0).toFixed(2)}</span>
                        <span>成本 {(pop.avgCost || 0).toFixed(2)}</span>
                        <span>精英 {pop.eliteCount || 0}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
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
                        <tr key={i} style={{ cursor: 'pointer' }}
                          onClick={() => t.decisionIri && viewGenealogy(t.decisionIri)}>
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
        /* ───── Genealogy Tab ───── */
        <div>
          {/* Decision Selector */}
          <div className="card mb-6">
            <div className="card-header">
              <div className="card-title">🔍 选择方案查看族谱</div>
            </div>
            <div style={{ padding: '12px 0' }}>
              <select
                style={{
                  width: '100%', padding: '8px 12px', borderRadius: 6,
                  border: '1px solid var(--border)', background: 'var(--bg-card)',
                  color: 'var(--text)', fontSize: 14,
                }}
                value={selectedDecisionIri || ''}
                onChange={e => setSelectedDecisionIri(e.target.value || null)}>
                <option value="">-- 请选择方案 --</option>
                {allDecisions.map(d => (
                  <option key={d.iri} value={d.iri}>{d.name}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Also show clickable cards for seed decisions from populations */}
          {Object.keys(popMembers).length > 0 && !selectedDecisionIri && (
            <div className="card mb-6">
              <div className="card-header">
                <div className="card-title">📋 种群中的方案</div>
              </div>
              <div style={{ padding: '8px 0' }}>
                {Object.entries(popMembers).flatMap(([conceptIri, members]) =>
                  members.map((m, i) => (
                    <div key={`${conceptIri}-${i}`} className="flex items-center gap-2"
                      style={{
                        padding: '8px 12px', cursor: 'pointer', borderRadius: 6,
                        borderBottom: i < members.length - 1 ? '1px solid var(--border-light)' : 'none',
                      }}
                      onClick={() => viewGenealogy(m.decisionIri)}>
                      <span style={{ fontWeight: 500, fontSize: 13, flex: 1 }}>{m.name}</span>
                      <span className="badge" style={{ fontSize: 11 }}>G{m.generation}</span>
                      <span style={{ color: 'var(--accent)', fontSize: 12 }}>查看族谱 →</span>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* Ancestry Tree */}
          {selectedDecisionIri ? (
            <AncestryTree decisionIri={selectedDecisionIri} traces={traces} />
          ) : (
            <div className="card" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>
              <div style={{ fontSize: 48, marginBottom: 12 }}>🌳</div>
              <p>请在上方选择一个方案</p>
              <p style={{ fontSize: 13, marginTop: 8 }}>或在"种群概览"中点击方案的"查看族谱"按钮</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function statusClass(s: string): string {
  switch (s) {
    case 'ACTIVE': case 'ELITE': return 'badge-success';
    case 'NEWBORN': return 'badge-primary';
    case 'PROBATION': return 'badge-warning';
    case 'DEPRECATED': return 'badge-danger';
    default: return '';
  }
}

function statusLabel(s: string): string {
  switch (s) {
    case 'ACTIVE': return '活跃';
    case 'ELITE': return '精英';
    case 'NEWBORN': return '新生';
    case 'PROBATION': return '观察';
    case 'DEPRECATED': return '废弃';
    default: return s;
  }
}
