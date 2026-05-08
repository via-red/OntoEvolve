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
  const [result, setResult] = useState<ActionEvent | null>(null);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ studentId: 'SGPF0001', description: BEHAVIOR_TEMPLATES[0].desc, severity: 'moderate', location: '教室' });
  const [copyIndex, setCopyIndex] = useState(-1);

  // Evaluation state
  const [evalForm, setEvalForm] = useState({ effectiveness: 0.7, cost: 0.3, satisfaction: 0.8 });
  const [evalSubmitting, setEvalSubmitting] = useState(false);
  const [evalDone, setEvalDone] = useState(false);

  // Candidate solutions selection
  const [candidates, setCandidates] = useState<any[]>([]);
  const [selectedIri, setSelectedIri] = useState('');
  const [candidatesLoading, setCandidatesLoading] = useState(false);

  useEffect(() => {
    loadEvents();
  }, []);

  const loadEvents = async () => {
    try {
      const evts = await api.getEvents();
      setEvents(evts);
    } catch (e) {}
    setLoading(false);
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    setResult(null);
    setError('');
    setEvalDone(false);
    setCandidates([]);
    setSelectedIri('');
    try {
      const res = await api.processEvent(form);
      setResult(res as ActionEvent);
      // Default to the auto-matched intervention
      setSelectedIri(res.interventionIri || '');
      // Use candidates from the response (backend now returns them directly)
      if (res.candidates && res.candidates.length > 0) {
        setCandidates(res.candidates);
      } else if (res.classifiedConcept) {
        // Fallback: fetch via API for backward compatibility
        setCandidatesLoading(true);
        try {
          const pop = await api.getPopulation(res.classifiedConcept);
          setCandidates(pop.members || []);
        } catch {}
        setCandidatesLoading(false);
      }
      await loadEvents();
    } catch (e: any) {
      setError(e.message || '事件处理失败');
    }
    setSubmitting(false);
  };

  const handleEvaluation = async () => {
    if (!result || !selectedIri) return;
    setEvalSubmitting(true);
    try {
      await api.submitEvaluation({
        interventionIri: selectedIri,
        studentId: result.studentId,
        ...evalForm,
      });
      setEvalDone(true);
    } catch (e: any) {
      setError('评价提交失败: ' + (e.message || ''));
    }
    setEvalSubmitting(false);
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
        <p>提交行为事件 → LLM 分类 → 选择方案 → 反馈评价</p>
      </div>

      <div className="grid grid-2">
        {/* Left: Event submission */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
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
          </div>

          {/* Processing Result */}
          {result && (
            <div className="card">
              <div className="card-header">
                <div className="card-title">处理结果</div>
              </div>

              <div className="pipeline" style={{ padding: '12px 0' }}>
                <div className="pipeline-step active" style={{ flex: 2, padding: '10px 12px' }}>
                  <div className="step-label">📥 事件</div>
                  <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginTop: 4, wordBreak: 'break-all' }}>
                    {(result.description || '').slice(0, 30)}...
                  </div>
                </div>
                <div className="pipeline-arrow">→</div>
                <div className="pipeline-step active" style={{ flex: 2, padding: '10px 12px' }}>
                  <div className="step-label">🏷️ 分类</div>
                  <div style={{ fontSize: 12, color: 'var(--primary-light)', marginTop: 4 }}>
                    {result.classifiedLabel || '—'}
                  </div>
                </div>
                <div className="pipeline-arrow">→</div>
                <div className="pipeline-step active" style={{ flex: 2, padding: '10px 12px' }}>
                  <div className="step-label">💊 推荐方案</div>
                  <div style={{ fontSize: 12, color: 'var(--accent)', marginTop: 4, fontWeight: 600 }}>
                    {result.matchedIntervention || result.suggestion || '—'}
                  </div>
                </div>
              </div>

              {/* Solution Selector */}
              {candidates.length > 0 && (
                <div style={{ borderTop: '1px solid var(--border-light)', paddingTop: 16, marginTop: 8 }}>
                  <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 4 }}>💊 选择实施方案</div>
                  <div style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 12 }}>
                    系统推荐了最佳方案，您也可以从种群中选择其他方案
                  </div>
                  {candidatesLoading ? (
                    <div className="loading" style={{ padding: 12 }}>加载候选方案...</div>
                  ) : (
                    <>
                      <select
                        value={selectedIri}
                        onChange={e => setSelectedIri(e.target.value)}
                        style={{
                          width: '100%', padding: '8px 12px', borderRadius: 6,
                          border: '1px solid var(--border)', background: 'var(--bg-card)',
                          color: 'var(--text)', fontSize: 13, marginBottom: 12,
                        }}>
                        {candidates.map((c, i) => (
                          <option key={i} value={c.decisionIri}>
                            {c.name} — 评分[{c.scoreVector?.map((s: number) => s.toFixed(2)).join(',') || '—'}] 尝试{c.trials}次
                          </option>
                        ))}
                      </select>
                      {/* Selected solution detail */}
                      {(() => {
                        const sel = candidates.find(c => c.decisionIri === selectedIri);
                        if (!sel) return null;
                        return (
                          <div style={{
                            background: 'var(--bg-card-hover)', borderRadius: 6, padding: 12, marginBottom: 12,
                            border: '1px solid var(--border-light)',
                          }}>
                            <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 8 }}>
                              <div><span style={{ color: 'var(--text-muted)', fontSize: 11 }}>代数 </span><span style={{ fontSize: 13, fontWeight: 500 }}>G{sel.generation}</span></div>
                              <div><span style={{ color: 'var(--text-muted)', fontSize: 11 }}>状态 </span><span className="badge" style={{ fontSize: 11 }}>{sel.status}</span></div>
                              <div><span style={{ color: 'var(--text-muted)', fontSize: 11 }}>尝试 </span><span style={{ fontSize: 13, fontWeight: 500 }}>{sel.trials} 次</span></div>
                            </div>
                            {sel.description && (
                              <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 6 }}>
                                {sel.description}
                              </div>
                            )}
                            {sel.steps && sel.steps.length > 0 && (
                              <div>
                                <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 4 }}>实施步骤：</div>
                                <ol style={{ margin: 0, paddingLeft: 18, fontSize: 12, color: 'var(--text-secondary)' }}>
                                  {sel.steps.map((s: string, i: number) => (
                                    <li key={i} style={{ marginBottom: 2 }}>{s}</li>
                                  ))}
                                </ol>
                              </div>
                            )}
                          </div>
                        );
                      })()}
                    </>
                  )}
                </div>
              )}

              {/* Evaluation Form */}
              {!evalDone ? (
                <div style={{ borderTop: '1px solid var(--border-light)', paddingTop: 16, marginTop: 8 }}>
                  <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>📝 评价反馈</div>
                  {[
                    { key: 'effectiveness', label: '效果 (Effectiveness)', emoji: '🎯', desc: '干预措施是否有效改善了行为' },
                    { key: 'cost', label: '成本 (Cost)', emoji: '💰', desc: '干预措施的资源消耗成本', reversed: true },
                    { key: 'satisfaction', label: '满意度 (Satisfaction)', emoji: '😊', desc: '学生和老师对干预的接受程度' },
                  ].map(item => (
                    <div key={item.key} style={{ marginBottom: 12 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
                        <label style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                          {item.emoji} {item.label}
                        </label>
                        <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--primary-light)' }}>
                          {((item.reversed ? 1 - (evalForm as any)[item.key] : (evalForm as any)[item.key]) * 100).toFixed(0)}%
                        </span>
                      </div>
                      <input type="range" min="0" max="1" step="0.05"
                        value={(evalForm as any)[item.key]}
                        onChange={e => setEvalForm(f => ({ ...f, [item.key]: parseFloat(e.target.value) }))}
                        style={{ width: '100%', accentColor: 'var(--primary)' }}
                      />
                      <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{item.desc}</div>
                    </div>
                  ))}
                  <button className="btn btn-primary" onClick={handleEvaluation} disabled={evalSubmitting} style={{ width: '100%', justifyContent: 'center' }}>
                    {evalSubmitting ? '提交中...' : '📊 提交评价'}
                  </button>
                </div>
              ) : (
                <div style={{ textAlign: 'center', padding: 16, color: 'var(--success)' }}>
                  ✅ 评价已提交，感谢反馈！
                </div>
              )}
            </div>
          )}

          {error && (
            <div className="code-block" style={{ color: 'var(--danger)', borderColor: 'rgba(239,68,68,0.3)' }}>
              ❌ {error}
            </div>
          )}
        </div>

        {/* Right: Event History */}
        <div className="card" style={{ overflow: 'auto', maxHeight: '80vh' }}>
          <div className="card-header">
            <div className="card-title">事件历史</div>
            <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{events.length} 条</span>
          </div>
          {loading ? (
            <div className="loading">加载中...</div>
          ) : events.length === 0 ? (
            <div className="card" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)', background: 'transparent' }}>
              <div style={{ fontSize: 48, marginBottom: 12 }}>📭</div>
              <p>暂无事件记录</p>
              <p style={{ fontSize: 13, marginTop: 8 }}>请在左侧提交行为事件</p>
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
                {[...events].reverse().map((e: any, idx) => (
                  <tr key={e.eventId || idx}>
                    <td><code style={{ fontSize: 11 }}>{e.studentId}</code></td>
                    <td>
                      <span className="badge badge-info">
                        {e.classifiedLabel || e.classifiedConcept?.split('#')[1] || '—'}
                      </span>
                    </td>
                    <td style={{ fontSize: 13, maxWidth: 160, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {e.matchedIntervention || '—'}
                    </td>
                    <td>{severityBadge(e.severity)}</td>
                    <td style={{ fontSize: 11, color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
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
