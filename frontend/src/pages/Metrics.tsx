import { useEffect, useState } from 'react';
import { api } from '../api';
import type { SystemMetrics } from '../types';

function Gauge({ value, label, max = 1, color, unit = '%' }: { value: number; label: string; max?: number; color: string; unit?: string }) {
  const hasData = value > 0;
  const pct = hasData ? Math.min((value / max) * 100, 100) : 0;
  return (
    <div style={{ textAlign: 'center' }}>
      <div style={{ position: 'relative', width: 100, height: 100, margin: '0 auto 8px' }}>
        <svg viewBox="0 0 36 36" style={{ width: '100%', height: '100%' }}>
          <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
            fill="none" stroke="var(--border-light)" strokeWidth="3" />
          {hasData && (
            <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              fill="none" stroke={color} strokeWidth="3"
              strokeDasharray={`${pct}, 100`}
              style={{ transition: 'stroke-dasharray 0.5s ease' }}
            />
          )}
        </svg>
        <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', fontSize: 16, fontWeight: 700 }}>
          {hasData ? `${(value * 100).toFixed(1)}%` : '—'}
        </div>
      </div>
      <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{label}</div>
    </div>
  );
}

export default function Metrics() {
  const [metrics, setMetrics] = useState<SystemMetrics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = () => api.getMetrics().then(setMetrics).catch(() => {}).finally(() => setLoading(false));
    load();
    const interval = setInterval(load, 5000);
    return () => clearInterval(interval);
  }, []);

  if (loading) return <div className="loading">加载指标数据...</div>;

  const m = metrics;
  const feedbackPerCall = m && m.llmCalls > 0 ? Math.min(m.totalFeedbacks / m.llmCalls, 1) : 0;
  const hypervolumeVal = m ? m.averageHypervolume : 0;
  const diversityVal = m ? m.nicheDiversity : 0;

  return (
    <div>
      <div className="page-header">
        <h2>📈 评估指标</h2>
        <p>系统运行状态监控 — 自动刷新（每 5 秒）</p>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-4 mb-6">
        <div className="stat-card">
          <div className="stat-label">📊 反馈总数</div>
          <div className="stat-value">{m?.totalFeedbacks ?? 0}</div>
          <div className="stat-desc">{(m?.totalFeedbacks ?? 0) > 0 ? '累积反馈驱动进化选择压力' : '暂无反馈数据'}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">🤖 LLM 调用</div>
          <div className="stat-value">{m?.llmCalls ?? 0}</div>
          <div className="stat-desc">{(m?.llmCalls ?? 0) > 0 ? 'DeepSeek API 调用次数' : '暂未调用'}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">🌿 生态位数量</div>
          <div className="stat-value">{m?.totalPopulations ?? 0}</div>
          <div className="stat-desc">{(m?.totalPopulations ?? 0) > 0 ? '活跃的种群数量' : '暂无生态位'}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">⚡ 事件总数</div>
          <div className="stat-value">{m?.totalEvents ?? 0}</div>
          <div className="stat-desc">{(m?.totalEvents ?? 0) > 0 ? '已处理的行为事件' : '暂未处理事件'}</div>
        </div>
      </div>

      <div className="grid grid-2">
        {/* Gauge Dashboard */}
        <div className="card">
          <div className="card-header">
            <div className="card-title">核心指标仪表盘</div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 16, padding: 12 }}>
            <Gauge value={hypervolumeVal} max={Math.max(hypervolumeVal * 2, 1)} label="Pareto 超体积" color="var(--primary-light)" />
            <Gauge value={diversityVal} label="生态位多样性" color="var(--accent)" />
            <Gauge value={feedbackPerCall} label="反馈/调用比" color="var(--success)" />
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <div className="card-title">指标说明</div>
          </div>
          <div style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 2 }}>
            {[
              { label: '超体积 (Hypervolume)', value: hypervolumeVal > 0 ? hypervolumeVal.toFixed(4) : '—', desc: '衡量 Pareto 前沿在目标空间中的覆盖范围。值越高表示前沿越优，方案多样性越好。' },
              { label: '生态位多样性 (NDI)', value: diversityVal > 0 ? (diversityVal * 100).toFixed(1) + '%' : '—', desc: '使用 Shannon 指数计算各生态位种群大小的均匀度。越接近 100% 表示资源分配越均衡。' },
              { label: '反馈/调用比', value: m?.llmCalls ? feedbackPerCall.toFixed(3) : '—', desc: '每次 LLM 调用产生的平均反馈数，衡量 LLM 生成方案的实际使用效率。' },
              { label: '进化代数', value: m?.totalPopulations ? '活跃中' : '—', desc: '每个生态位种群经历的进化循环次数。代数越多表示该生态位的方案经过更多轮优化。' },
            ].map((item, i) => (
              <div key={i} style={{ padding: '6px 0', borderBottom: '1px solid var(--border-light)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <strong style={{ color: 'var(--text)' }}>{item.label}</strong>
                  <span style={{ color: 'var(--primary-light)', fontWeight: 600, fontSize: 12 }}>{item.value}</span>
                </div>
                <p style={{ margin: '2px 0 0' }}>{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Configuration Parameters */}
      <div className="card mt-6">
        <div className="card-header">
          <div className="card-title">系统运行参数</div>
        </div>
        {!m || (m.totalPopulations === 0 && m.totalFeedbacks === 0 && m.llmCalls === 0) ? (
          <div style={{ textAlign: 'center', padding: 32, color: 'var(--text-muted)', fontSize: 14 }}>
            暂无运行数据，参数将在系统运行后显示
          </div>
        ) : (
          <div className="grid grid-3" style={{ gap: 16, fontSize: 13 }}>
            {[
              {
                label: '变异算子权重',
                items: [
                  `LLM 生成: ${m && m.llmCalls > 10 ? '2' : '—'}`,
                  `交叉重组: ${m && m.totalFeedbacks > 5 ? '3' : '—'}`,
                  `微扰变异: 1`,
                ]
              },
              {
                label: '种群参数',
                items: [
                  `默认容量: 12`,
                  `触发阈值: 10 反馈/生态位`,
                  `当前种群数: ${m?.totalPopulations ?? 0}`,
                ]
              },
              {
                label: '运行统计',
                items: [
                  `反馈总数: ${m?.totalFeedbacks ?? 0}`,
                  `LLM 调用: ${m?.llmCalls ?? 0}`,
                  `超体积均值: ${hypervolumeVal > 0 ? hypervolumeVal.toFixed(4) : '—'}`,
                ]
              },
              {
                label: '迁移配置',
                items: [
                  `兼容阈值: 0.8`,
                  `跨生态位精英迁移`,
                  `多样性指数: ${diversityVal > 0 ? (diversityVal * 100).toFixed(1) + '%' : '—'}`,
                ]
              },
              {
                label: '元进化',
                items: [
                  `优化参数: explorationRate, populationCapacity`,
                  `目标指标: avgHypervolume, nicheDiversity`,
                ]
              },
              {
                label: '选择策略',
                items: [
                  `算子: ParetoCrowdingSelector`,
                  `匹配: ParetoUCBMatcher`,
                  `${m?.llmCalls > 0 ? '✅ 已激活' : '⏳ 待运行'}`,
                ]
              },
            ].map((group, i) => (
              <div key={i} className="card" style={{ padding: 14 }}>
                <div style={{ fontWeight: 600, marginBottom: 6, fontSize: 13 }}>{group.label}</div>
                {group.items.map((item, j) => (
                  <div key={j} style={{ color: 'var(--text-secondary)', padding: '2px 0', fontSize: 12 }}>{item}</div>
                ))}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
