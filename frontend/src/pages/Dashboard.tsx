import { useEffect, useState } from 'react';
import { api } from '../api';
import type { DashboardData } from '../types';

export default function Dashboard() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => { loadDashboard(); }, []);

  const loadDashboard = async () => {
    setLoading(true);
    setError('');
    try {
      const d = await api.getDashboard();
      setData(d);
    } catch {
      setError('无法加载仪表盘数据，请确认后端服务已启动');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">加载中...</div>;
  if (error) return (
    <div className="card" style={{ textAlign: 'center', padding: 60 }}>
      <p style={{ color: 'var(--text-secondary)', marginBottom: 16 }}>{error}</p>
      <button className="btn" onClick={loadDashboard}>重试</button>
    </div>
  );
  if (!data) return null;

  const { stats, recentActivity, topInterventions, nicheHealth } = data;

  return (
    <div>
      <div className="page-header">
        <h2>工作台</h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginTop: 4 }}>系统运行概览与实时动态</p>
      </div>

      <div className="grid grid-4">
        <StatCard label="📥 今日事件" value={stats.todayEvents} desc="今日提交的行为事件" />
        <StatCard label="⭐ 待评价" value={stats.pendingEvaluations} desc="尚未提交效果评价" />
        <StatCard label="💡 活跃方案" value={stats.activeInterventions} desc="所有生态位中的活跃方案" />
        <StatCard label="🧬 进化代次" value={`Gen ${stats.totalGenerations}`} desc="最高进化代际" />
        <StatCard label="📝 累积反馈" value={stats.totalFeedbacks} desc="收到的效果评价总数" />
        <StatCard label="🌳 生态位" value={stats.totalPopulations} desc="本体概念生态位数量" />
        <StatCard label="👨‍🎓 学生" value={stats.totalStudents} desc="已导入学生数据" />
        <StatCard label="🤖 LLM调用" value={stats.llmCalls} desc="大模型调用总次数" />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24, marginTop: 24 }}>
        <div className="card">
          <div className="card-header">
            <div className="card-title">最近活动</div>
          </div>
          <div style={{ maxHeight: 360, overflowY: 'auto' }}>
            {recentActivity.length === 0 ? (
              <EmptyHint text="暂无活动记录" hint="提交一条行为事件后开始" />
            ) : (
              recentActivity.map((item) => (
                <div key={item.eventId} style={{
                  padding: '10px 20px', borderBottom: '1px solid var(--border)',
                  fontSize: 13, display: 'flex', alignItems: 'center', gap: 10
                }}>
                  <span style={{
                    background: item.severity === 'severe' ? 'var(--accent-alt)' :
                      item.severity === 'moderate' ? 'var(--decide)' : 'var(--act)',
                    color: '#fff', borderRadius: 4, padding: '1px 6px', fontSize: 10, flexShrink: 0
                  }}>
                    {item.severity === 'severe' ? '严重' : item.severity === 'moderate' ? '中等' : '轻微'}
                  </span>
                  <span style={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    <span style={{ color: 'var(--text-muted)', fontSize: 11 }}>{item.studentId} · </span>
                    {item.description?.length > 20 ? item.description.slice(0, 20) + '...' : item.description}
                  </span>
                  {item.matchedIntervention ? (
                    <span style={{ color: 'var(--primary)', fontSize: 11, flexShrink: 0 }}>
                      → {item.matchedIntervention}
                    </span>
                  ) : null}
                  {item.hasEvaluation ? <span style={{ fontSize: 10 }}>✅</span> : <span style={{ fontSize: 10 }}>⏳</span>}
                </div>
              ))
            )}
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <div className="card-title">方案效果排行</div>
          </div>
          <div style={{ maxHeight: 360, overflowY: 'auto' }}>
            {topInterventions.length === 0 ? (
              <EmptyHint text="暂无方案评分" hint="提交评价后产生排行" />
            ) : (
              topInterventions.map((item, i) => (
                <div key={item.iri} style={{
                  padding: '10px 20px', borderBottom: '1px solid var(--border)', fontSize: 13
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>
                      <span style={{ color: 'var(--text-muted)', marginRight: 8 }}>#{i + 1}</span>
                      <strong>{item.name}</strong>
                      <span style={{ color: 'var(--text-muted)', fontSize: 11, marginLeft: 6 }}>
                        {item.conceptLabel} · {item.trials}次
                      </span>
                    </span>
                    <span style={{ color: 'var(--primary)', fontWeight: 600 }}>
                      {item.effectiveness.toFixed(2)}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: 24 }}>
        <div className="card-header">
          <div className="card-title">生态位健康概览</div>
        </div>
        <div style={{ padding: '0 20px 16px' }}>
          {nicheHealth.length === 0 ? (
            <EmptyHint text="暂无生态位数据" hint="提交事件后自动创建生态位" />
          ) : (
            nicheHealth.map((nh) => (
              <div key={nh.conceptIri} style={{
                display: 'flex', alignItems: 'center', gap: 16, padding: '10px 0',
                borderBottom: '1px solid var(--border)'
              }}>
                <span style={{ width: 100, fontWeight: 600, fontSize: 13 }}>{nh.conceptLabel}</span>
                <span style={{ fontSize: 12, color: 'var(--text-secondary)', width: 60 }}>Gen {nh.generation}</span>
                <span style={{ fontSize: 12, color: 'var(--text-secondary)', width: 60 }}>{nh.populationSize} 方案</span>
                <span style={{ fontSize: 12, color: 'var(--text-secondary)', width: 70 }}>
                  ELITE {nh.eliteCount}
                </span>
                <div style={{ flex: 1 }}>
                  <Bar label="效果" value={nh.avgEffectiveness} color="var(--primary)" />
                  <Bar label="成本" value={nh.avgCost} color="var(--decide)" />
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

function StatCard({ label, value, desc }: { label: string; value: string | number; desc: string }) {
  return (
    <div className="stat-card">
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      <div className="stat-desc">{desc}</div>
    </div>
  );
}

function Bar({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 11 }}>
      <span style={{ color: 'var(--text-muted)', width: 24 }}>{label}</span>
      <div style={{ flex: 1, height: 6, background: 'var(--border)', borderRadius: 3 }}>
        <div style={{ width: `${(value * 100).toFixed(0)}%`, height: '100%', background: color, borderRadius: 3 }} />
      </div>
      <span style={{ width: 36, textAlign: 'right' }}>{(value * 100).toFixed(0)}%</span>
    </div>
  );
}

function EmptyHint({ text, hint }: { text: string; hint: string }) {
  return (
    <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>
      <p style={{ fontSize: 14 }}>{text}</p>
      <p style={{ fontSize: 12, marginTop: 4 }}>{hint}</p>
    </div>
  );
}
