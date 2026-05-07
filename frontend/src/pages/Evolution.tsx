import { useEffect, useState } from 'react';
import { api } from '../api';
import type { PopulationView, EvolTrace } from '../types';

export default function Evolution() {
  const [populations, setPopulations] = useState<PopulationView[]>([]);
  const [traces, setTraces] = useState<EvolTrace[]>([]);
  const [loading, setLoading] = useState(true);
  const [evolving, setEvolving] = useState<string | null>(null);

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
        <p>生态位种群监控 — 查看各概念下的种群状态和进化轨迹</p>
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

      {populations.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>
          暂无种群数据。请先在"事件处理"页面提交行为事件，系统会自动创建种群。
        </div>
      ) : (
        <div className="grid" style={{ gap: 20 }}>
          {populations.map(pop => (
            <div key={pop.conceptIri} className="card">
              <div className="card-header">
                <div className="flex items-center gap-2">
                  <span className="node-dot" style={{ background: categoryColor(pop.conceptCategory), width: 12, height: 12 }} />
                  <div className="card-title">{pop.conceptLabel}</div>
                  <span className={`badge ${pop.conceptCategory === 'Behavioral' ? 'badge-danger' : pop.conceptCategory === 'Academic' ? 'badge-info' : 'badge-success'}`}
                    style={{ background: `${categoryColor(pop.conceptCategory)}20`, color: categoryColor(pop.conceptCategory) }}>
                    {pop.conceptCategory === 'Behavioral' ? '行为' : pop.conceptCategory === 'Academic' ? '学业' : '社交'}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                    第 {pop.generation} 代 · {pop.size} 个方案 · {pop.activeCount} 活跃
                  </span>
                  <button className="btn btn-sm btn-primary" onClick={() => triggerEvolve(pop.conceptIri)} disabled={evolving === pop.conceptIri}>
                    {evolving === pop.conceptIri ? '进化中...' : '触发进化'}
                  </button>
                </div>
              </div>

              <div style={{ padding: '12px 0', fontSize: 13, color: 'var(--text-secondary)', textAlign: 'center' }}>
                种群规模: {pop.size} · 活跃个体: {pop.activeCount} · 当前代数: {pop.generation}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 进化轨迹 */}
      {traces.length > 0 && (
        <div className="card mt-6">
          <div className="card-header">
            <div className="card-title">进化轨迹</div>
            <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{traces.length} 条记录</span>
          </div>
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>操作类型</th>
                  <th>方案</th>
                  <th>上下文</th>
                  <th>时间</th>
                </tr>
              </thead>
              <tbody>
                {traces.slice().reverse().map((t, i) => (
                  <tr key={i}>
                    <td>
                      <span className={`badge ${t.type === 'LLM_GENERATE' ? 'badge-primary' : t.type === 'CROSSOVER' ? 'badge-info' : 'badge-warning'}`}>
                        {t.type}
                      </span>
                    </td>
                    <td style={{ fontSize: 13 }}>{t.decision}</td>
                    <td style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{(t.context || '').slice(0, 60)}</td>
                    <td style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                      {t.timestamp ? new Date(t.timestamp).toLocaleString('zh-CN') : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
