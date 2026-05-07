import { useEffect, useState } from 'react';
import { api } from '../api';
import type { SystemMetrics } from '../types';

function Gauge({ value, label, max = 1, color }: { value: number; label: string; max?: number; color: string }) {
  const pct = Math.min((value / max) * 100, 100);
  return (
    <div style={{ textAlign: 'center' }}>
      <div style={{ position: 'relative', width: 100, height: 100, margin: '0 auto 8px' }}>
        <svg viewBox="0 0 36 36" style={{ width: '100%', height: '100%' }}>
          <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
            fill="none" stroke="var(--border-light)" strokeWidth="3" />
          <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
            fill="none" stroke={color} strokeWidth="3"
            strokeDasharray={`${pct}, 100`} />
        </svg>
        <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', fontSize: 16, fontWeight: 700 }}>
          {(value * 100).toFixed(1)}%
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

  return (
    <div>
      <div className="page-header">
        <h2>📈 评估指标</h2>
        <p>系统运行状态监控 — 自动刷新（每 5 秒）</p>
      </div>

      <div className="grid grid-4 mb-6">
        <div className="stat-card">
          <div className="stat-label">反馈总数</div>
          <div className="stat-value">{metrics?.totalFeedbacks ?? 0}</div>
          <div className="stat-desc">累积反馈驱动进化选择压力</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">LLM 调用</div>
          <div className="stat-value">{metrics?.llmCalls ?? 0}</div>
          <div className="stat-desc">DeepSeek API 调用次数</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">超体积均值</div>
          <div className="stat-value">{(metrics?.averageHypervolume ?? 0).toFixed(3)}</div>
          <div className="stat-desc">Pareto 前沿质量指标</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">生态位多样性</div>
          <div className="stat-value">{((metrics?.nicheDiversity ?? 0) * 100).toFixed(1)}%</div>
          <div className="stat-desc">Shannon 指数归一化值</div>
        </div>
      </div>

      <div className="grid grid-2">
        <div className="card">
          <div className="card-header">
            <div className="card-title">核心指标仪表盘</div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 16, padding: 12 }}>
            <Gauge value={metrics ? Math.min(metrics.averageHypervolume * 3, 1) : 0} label="种群质量" color="var(--primary-light)" />
            <Gauge value={metrics?.nicheDiversity ?? 0} label="多样性指数" color="var(--accent)" />
            <Gauge value={metrics && metrics.llmCalls > 0 ? Math.min(metrics.totalFeedbacks / Math.max(metrics.llmCalls, 1), 1) : 0} label="反馈/调用比" color="var(--success)" />
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <div className="card-title">指标说明</div>
          </div>
          <div style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 2 }}>
            {[
              { label: '超体积 (Hypervolume)', desc: '衡量 Pareto 前沿在目标空间中的覆盖范围。值越高表示前沿越优，方案多样性越好。参考点为当前种群中最差值 -0.1。' },
              { label: '生态位多样性 (NDI)', desc: '使用 Shannon 指数计算各生态位种群大小的均匀度。值越接近 100% 表示资源在各生态位间分配越均衡。' },
              { label: '反馈/调用比', desc: '每次 LLM 调用产生的平均反馈数。衡量 LLM 生成方案的实际使用效率。' },
              { label: '进化代数', desc: '每个生态位种群经历的进化循环次数。代数越多表示该生态位的方案经过更多轮优化。' },
            ].map((item, i) => (
              <div key={i} style={{ padding: '6px 0', borderBottom: '1px solid var(--border-light)' }}>
                <strong style={{ color: 'var(--text)' }}>{item.label}</strong>
                <p style={{ margin: 0 }}>{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="card mt-6">
        <div className="card-header">
          <div className="card-title">进化计算参数</div>
        </div>
        <div className="grid grid-3" style={{ gap: 16, fontSize: 13 }}>
          {[
            { label: '变异算子权重', items: ['LLM 生成: 2', '交叉重组: 3', '微扰变异: 1'] },
            { label: '种群参数', items: ['默认容量: 12', '触发阈值: 10 反馈/生态位'] },
            { label: '学习率', items: ['探索率 (explorationRate): 0.15', '信用衰减 (λ): 0.9', '最大回溯: 30 步'] },
            { label: '迁移配置', items: ['兼容阈值: 0.8', '跨生态位精英迁移'] },
            { label: '元进化', items: ['优化参数: explorationRate, populationCapacity', '目标指标: avgHypervolume, nicheDiversity'] },
            { label: '选择策略', items: ['算子: ParetoCrowdingSelector', '匹配: ParetoUCBMatcher'] },
          ].map((group, i) => (
            <div key={i} className="card" style={{ padding: 14 }}>
              <div style={{ fontWeight: 600, marginBottom: 6, fontSize: 13 }}>{group.label}</div>
              {group.items.map((item, j) => (
                <div key={j} style={{ color: 'var(--text-secondary)', padding: '2px 0', fontSize: 12 }}>{item}</div>
              ))}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
