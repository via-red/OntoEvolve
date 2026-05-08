import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import type { SystemMetrics } from '../types';

export default function Dashboard() {
  const [metrics, setMetrics] = useState<SystemMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api.getMetrics()
      .then(setMetrics)
      .catch(() => setError('无法连接后端服务'))
      .finally(() => setLoading(false));
  }, []);

  const fmt = (v: number, d = 2) => (v || 0).toFixed(d);

  const stats = metrics ? [
    { label: '学生总数', value: metrics.totalStudents, desc: 'UCI Student Performance 数据集', icon: '👨‍🎓', highlight: false },
    { label: '事件总数', value: metrics.totalEvents, desc: metrics.totalEvents > 0 ? '已处理的行为事件' : '暂未处理事件', icon: '⚡', highlight: false },
    { label: '干预方案', value: metrics.totalInterventions, desc: metrics.totalInterventions > 0 ? '进化产生的方案数' : '暂无干预方案', icon: '💊', highlight: false },
    { label: '反馈评价', value: metrics.totalFeedbacks, desc: metrics.totalFeedbacks > 0 ? '驱动的进化选择压力' : '暂无反馈', icon: '📝', highlight: false },
    { label: 'LLM 调用', value: metrics.llmCalls, desc: 'DeepSeek API 调用次数', icon: '🤖', highlight: false },
    { label: '生态位数量', value: metrics.totalPopulations, desc: metrics.totalPopulations > 0 ? '活跃的概念种群' : '暂无生态位', icon: '🌿', highlight: false },
    { label: '种群多样性', value: (metrics.nicheDiversity * 100).toFixed(1) + '%', desc: metrics.nicheDiversity > 0 ? 'Shannon 多样性指数' : '暂无数据', icon: '🔄', highlight: metrics.nicheDiversity > 0 },
    { label: '超体积均值', value: metrics.averageHypervolume > 0 ? fmt(metrics.averageHypervolume) : '—', desc: metrics.averageHypervolume > 0 ? 'Pareto 前沿质量' : '暂无数据', icon: '📐', highlight: metrics.averageHypervolume > 0 },
  ] : [];

  const hasData = metrics && (metrics.totalEvents > 0 || metrics.totalFeedbacks > 0 || metrics.totalPopulations > 0);

  return (
    <div>
      <div className="page-header">
        <h2>系统仪表盘</h2>
        <p>OntoEvolve 教育决策进化系统 — 本体驱动的自进化决策框架</p>
      </div>

      {loading ? (
        <div className="loading">加载中...</div>
      ) : error ? (
        <div className="card" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>
          <div style={{ fontSize: 48, marginBottom: 16 }}>🔌</div>
          <p>{error}</p>
          <p style={{ fontSize: 13, marginTop: 8 }}>请确保后端服务已启动</p>
        </div>
      ) : (
        <>
          <div className="grid grid-4 mb-6">
            {stats.map((s, i) => (
              <div key={i} className={`stat-card ${s.highlight ? '' : ''}`}
                style={!hasData && i < 2 ? { opacity: 0.6 } : undefined}>
                <div className="stat-label">{s.icon} {s.label}</div>
                <div className="stat-value" style={{ fontSize: s.value?.toString()?.length > 6 ? 22 : 28 }}>
                  {s.value ?? '—'}
                </div>
                <div className="stat-desc">{s.desc}</div>
              </div>
            ))}
          </div>

          {!hasData && (
            <div className="card mb-6" style={{ background: 'rgba(79,70,229,0.06)', borderColor: 'rgba(79,70,229,0.2)', textAlign: 'center', padding: 24 }}>
              <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>
                💡 系统暂无运行数据，请前往 <Link to="/events">事件处理</Link> 页面提交行为事件开始使用
              </p>
            </div>
          )}

          <div className="grid grid-2">
            <div className="card">
              <div className="card-header">
                <div className="card-title">系统架构</div>
              </div>
              <div className="pipeline" style={{ justifyContent: 'center' }}>
                {[
                  { icon: '📥', label: '输入事件' },
                  { icon: '🏷️', label: 'LLM 分类' },
                  { icon: '🎯', label: '匹配方案' },
                  { icon: '🧬', label: '进化优化' },
                  { icon: '📊', label: '反馈评价' },
                ].map((step, i) => (
                  <span key={i} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <span className="pipeline-step active" style={{ padding: '8px 12px', minWidth: 80 }}>
                      <div className="step-icon" style={{ fontSize: 20 }}>{step.icon}</div>
                      <div className="step-label">{step.label}</div>
                    </span>
                    {i < 4 && <span className="pipeline-arrow" style={{ fontSize: 16 }}>→</span>}
                  </span>
                ))}
              </div>
              <div className="explain-box mt-4">
                <p>
                  <strong>OntoEvolve</strong> 是一个本体驱动的自进化决策框架。
                  行为事件经过 <strong>LLM 语义分类</strong> 映射到本体概念生态位，
                  从种群中 <strong>匹配</strong> 最优干预方案，
                  通过 <strong>进化算法</strong>（变异+选择+迁移）持续优化方案质量，
                  最后通过 <strong>多维反馈</strong> 驱动选择压力。
                </p>
              </div>
            </div>

            <div className="card">
              <div className="card-header">
                <div className="card-title">快速入口</div>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {[
                  { to: '/how-it-works', icon: '📖', title: '原理说明', desc: '了解系统的完整工作流程' },
                  { to: '/events', icon: '⚡', title: '处理事件', desc: '提交行为事件并查看处理结果' },
                  { to: '/evolution', icon: '🧬', title: '进化引擎', desc: '查看种群进化状态和族谱' },
                  { to: '/metrics', icon: '📈', title: '评估指标', desc: '查看系统运行指标和图表' },
                ].map(item => (
                  <Link key={item.to} to={item.to} style={{
                    padding: '12px 16px', background: 'var(--bg-card-hover)',
                    borderRadius: 'var(--radius-sm)', display: 'flex', alignItems: 'center', gap: 12
                  }}>
                    <span style={{ fontSize: 24 }}>{item.icon}</span>
                    <div>
                      <div style={{ fontWeight: 600 }}>{item.title}</div>
                      <div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{item.desc}</div>
                    </div>
                  </Link>
                ))}
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
