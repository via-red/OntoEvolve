import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import type { SystemMetrics } from '../types';

export default function Dashboard() {
  const [metrics, setMetrics] = useState<SystemMetrics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.getMetrics().then(setMetrics).catch(() => {}).finally(() => setLoading(false));
  }, []);

  const stats = metrics ? [
    { label: '学生总数', value: metrics.totalStudents, desc: '来自 UCI Student Performance 数据集', icon: '👨‍🎓' },
    { label: '事件总数', value: metrics.totalEvents, desc: '已处理的行为事件', icon: '⚡' },
    { label: '干预方案', value: metrics.totalInterventions, desc: '进化产生的方案数', icon: '💊' },
    { label: '反馈评价', value: metrics.totalFeedbacks, desc: '驱动的进化选择压力', icon: '📝' },
    { label: 'LLM 调用', value: metrics.llmCalls, desc: 'DeepSeek API 调用次数', icon: '🤖' },
    { label: '生态位数量', value: metrics.totalPopulations, desc: '活跃的概念种群', icon: '🌿' },
    { label: '种群多样性', value: (metrics.nicheDiversity * 100).toFixed(1) + '%', desc: 'Shannon 多样性指数', icon: '🔄' },
    { label: '超体积均值', value: metrics.averageHypervolume.toFixed(3), desc: 'Pareto 前沿质量', icon: '📐' },
  ] : [];

  return (
    <div>
      <div className="page-header">
        <h2>系统仪表盘</h2>
        <p>OntoEvolve 教育决策进化系统 — 本体驱动的自进化决策框架</p>
      </div>

      {loading ? (
        <div className="loading">加载中...</div>
      ) : (
        <>
          <div className="grid grid-4 mb-6">
            {stats.map((s, i) => (
              <div key={i} className="stat-card">
                <div className="stat-label">{s.icon} {s.label}</div>
                <div className="stat-value">{s.value}</div>
                <div className="stat-desc">{s.desc}</div>
              </div>
            ))}
          </div>

          <div className="grid grid-2">
            <div className="card">
              <div className="card-header">
                <div className="card-title">系统架构</div>
              </div>
              <div className="pipeline" style={{ justifyContent: 'center' }}>
                <div className="pipeline-step active">
                  <div className="step-icon">📥</div>
                  <div className="step-label">输入事件</div>
                </div>
                <div className="pipeline-arrow">→</div>
                <div className="pipeline-step">
                  <div className="step-icon">🏷️</div>
                  <div className="step-label">LLM 分类</div>
                </div>
                <div className="pipeline-arrow">→</div>
                <div className="pipeline-step">
                  <div className="step-icon">🎯</div>
                  <div className="step-label">匹配方案</div>
                </div>
                <div className="pipeline-arrow">→</div>
                <div className="pipeline-step">
                  <div className="step-icon">🧬</div>
                  <div className="step-label">进化优化</div>
                </div>
                <div className="pipeline-arrow">→</div>
                <div className="pipeline-step">
                  <div className="step-icon">📊</div>
                  <div className="step-label">反馈评价</div>
                </div>
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
                <Link to="/how-it-works" style={{ padding: '12px 16px', background: 'var(--bg-card-hover)', borderRadius: 'var(--radius-sm)', display: 'flex', alignItems: 'center', gap: 12 }}>
                  <span style={{ fontSize: 24 }}>📖</span>
                  <div><div style={{ fontWeight: 600 }}>原理说明</div><div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>了解系统的完整工作流程</div></div>
                </Link>
                <Link to="/events" style={{ padding: '12px 16px', background: 'var(--bg-card-hover)', borderRadius: 'var(--radius-sm)', display: 'flex', alignItems: 'center', gap: 12 }}>
                  <span style={{ fontSize: 24 }}>⚡</span>
                  <div><div style={{ fontWeight: 600 }}>处理事件</div><div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>提交行为事件并查看处理结果</div></div>
                </Link>
                <Link to="/evolution" style={{ padding: '12px 16px', background: 'var(--bg-card-hover)', borderRadius: 'var(--radius-sm)', display: 'flex', alignItems: 'center', gap: 12 }}>
                  <span style={{ fontSize: 24 }}>🧬</span>
                  <div><div style={{ fontWeight: 600 }}>进化引擎</div><div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>查看种群进化状态和干预方案</div></div>
                </Link>
                <Link to="/metrics" style={{ padding: '12px 16px', background: 'var(--bg-card-hover)', borderRadius: 'var(--radius-sm)', display: 'flex', alignItems: 'center', gap: 12 }}>
                  <span style={{ fontSize: 24 }}>📈</span>
                  <div><div style={{ fontWeight: 600 }}>评估指标</div><div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>查看系统运行指标和图表</div></div>
                </Link>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
