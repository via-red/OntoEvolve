import { useEffect, useState } from 'react';
import { api } from '../api';
import type { ActionEvent } from '../types';

const BEHAVIOR_TEMPLATES = [
  { desc: '上课大声喧哗，多次打断老师讲课', sev: 'moderate', loc: '教室' },
  { desc: '与同桌发生争吵，互相推搡', sev: 'moderate', loc: '教室' },
  { desc: '拒绝按照老师要求调换座位', sev: 'mild', loc: '教室' },
  { desc: '在班级群里发布同学的恶意P图', sev: 'severe', loc: '网络' },
  { desc: '连续三天未交数学作业', sev: 'mild', loc: '教室' },
  { desc: '期中考试抄袭邻座答案被监考老师发现', sev: 'severe', loc: '考场' },
  { desc: '月考成绩从85分降至60分', sev: 'moderate', loc: '教室' },
  { desc: '上课经常走神发呆，笔记空白', sev: 'mild', loc: '教室' },
  { desc: '拒绝参加班级集体活动', sev: 'mild', loc: '操场' },
  { desc: '故意踢坏教室门', sev: 'severe', loc: '走廊' },
];

export default function Events() {
  const [events, setEvents] = useState<ActionEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<any>(null);
  const [form, setForm] = useState({ studentId: 'SGPF0001', description: BEHAVIOR_TEMPLATES[0].desc, severity: 'moderate', location: '教室' });
  const [copyIndex, setCopyIndex] = useState(-1);

  useEffect(() => {
    api.getEvents().then(setEvents).catch(() => {}).finally(() => setLoading(false));
  }, []);

  const handleSubmit = async () => {
    setSubmitting(true);
    setResult(null);
    try {
      const res = await api.processEvent(form);
      setResult(res);
      const evts = await api.getEvents();
      setEvents(evts);
    } catch (e: any) {
      setResult({ error: e.message });
    }
    setSubmitting(false);
  };

  const pickTemplate = (idx: number) => {
    const t = BEHAVIOR_TEMPLATES[idx];
    setForm(f => ({ ...f, description: t.desc, severity: t.sev, location: t.loc }));
    setCopyIndex(idx);
  };

  const severityBadge = (s: string) => {
    const map: Record<string, string> = { mild: 'badge-success', moderate: 'badge-warning', severe: 'badge-danger', critical: 'badge-danger' };
    const labels: Record<string, string> = { mild: '轻微', moderate: '中等', severe: '严重', critical: '危急' };
    return <span className={`badge ${map[s] || ''}`}>{labels[s] || s}</span>;
  };

  return (
    <div>
      <div className="page-header">
        <h2>⚡ 事件处理</h2>
        <p>提交行为事件 → LLM 分类 → 方案匹配 → 查看处理结果</p>
      </div>

      <div className="grid grid-2">
        {/* 事件提交表单 */}
        <div className="card">
          <div className="card-header">
            <div className="card-title">提交行为事件</div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div>
              <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>学生 ID</label>
              <input type="text" value={form.studentId}
                onChange={e => setForm(f => ({ ...f, studentId: e.target.value }))}
                style={{ width: '100%', padding: '8px 12px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-light)', background: 'var(--bg-card)', color: 'var(--text)', fontSize: 14 }}
              />
            </div>

            <div>
              <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>行为描述</label>
              <textarea value={form.description}
                onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                rows={3}
                style={{ width: '100%', padding: '8px 12px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-light)', background: 'var(--bg-card)', color: 'var(--text)', fontSize: 14, resize: 'vertical' }}
              />
            </div>

            <div className="flex gap-2 flex-wrap">
              {BEHAVIOR_TEMPLATES.map((t, i) => (
                <button key={i} className={`btn btn-sm ${copyIndex === i ? 'btn-primary' : ''}`}
                  style={{ background: copyIndex === i ? undefined : 'var(--bg-card-hover)', color: copyIndex === i ? undefined : 'var(--text-secondary)' }}
                  onClick={() => pickTemplate(i)}>
                  {t.desc.slice(0, 12)}...
                </button>
              ))}
            </div>

            <div className="flex gap-4">
              <div style={{ flex: 1 }}>
                <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>严重程度</label>
                <select value={form.severity}
                  onChange={e => setForm(f => ({ ...f, severity: e.target.value }))}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-light)', background: 'var(--bg-card)', color: 'var(--text)', fontSize: 14 }}>
                  <option value="mild">轻微</option>
                  <option value="moderate">中等</option>
                  <option value="severe">严重</option>
                </select>
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>地点</label>
                <input type="text" value={form.location}
                  onChange={e => setForm(f => ({ ...f, location: e.target.value }))}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-light)', background: 'var(--bg-card)', color: 'var(--text)', fontSize: 14 }}
                />
              </div>
            </div>

            <button className="btn btn-primary" onClick={handleSubmit} disabled={submitting}>
              {submitting ? '处理中...' : '🚀 提交事件'}
            </button>
          </div>

          {/* 处理结果 */}
          {result && (
            <div className="mt-4">
              <div className="card-title mb-4">处理结果</div>
              {result.error ? (
                <div className="code-block" style={{ color: 'var(--danger)' }}>错误: {result.error}</div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <div className="pipeline">
                    <div className="pipeline-step active" style={{ flex: 2 }}>
                      <div className="step-label">📥 事件</div>
                      <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 4 }}>{result.eventId?.slice(0, 20)}...</div>
                    </div>
                    <div className="pipeline-arrow">→</div>
                    <div className="pipeline-step active" style={{ flex: 2 }}>
                      <div className="step-label">🏷️ 分类</div>
                      <div style={{ fontSize: 12, color: 'var(--primary-light)', marginTop: 4 }}>{result.classifiedConcept || '—'}</div>
                    </div>
                    <div className="pipeline-arrow">→</div>
                    <div className="pipeline-step active" style={{ flex: 2 }}>
                      <div className="step-label">💊 方案</div>
                      <div style={{ fontSize: 12, color: 'var(--accent)', marginTop: 4, fontWeight: 600 }}>{result.suggestion}</div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* 事件历史 */}
        <div className="card" style={{ overflow: 'auto', maxHeight: '70vh' }}>
          <div className="card-header">
            <div className="card-title">事件历史</div>
            <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{events.length} 条</span>
          </div>
          {loading ? (
            <div className="loading">加载中...</div>
          ) : events.length === 0 ? (
            <div style={{ color: 'var(--text-muted)', fontSize: 14, textAlign: 'center', padding: 40 }}>
              暂无事件记录，请在上方提交事件
            </div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>学生</th>
                  <th>分类</th>
                  <th>方案</th>
                  <th>严重度</th>
                  <th>时间</th>
                </tr>
              </thead>
              <tbody>
                {events.slice().reverse().map(e => (
                  <tr key={e.eventId}>
                    <td><code style={{ fontSize: 11 }}>{e.studentId}</code></td>
                    <td><span className="badge badge-info">{e.classifiedLabel || e.classifiedConcept?.split('#')[1] || '—'}</span></td>
                    <td style={{ fontSize: 13 }}>{e.matchedIntervention || '—'}</td>
                    <td>{severityBadge(e.severity)}</td>
                    <td style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                      {e.timestamp ? new Date(e.timestamp).toLocaleString('zh-CN') : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
