import { useEffect, useState, useCallback } from 'react';
import { api } from '../api';
import type { ActionEvent, EventsPage, OntologyNode } from '../types';

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

const SEVERITY_OPTIONS = [
  { value: '', label: '全部严重度' },
  { value: 'mild', label: '轻微' },
  { value: 'moderate', label: '中等' },
  { value: 'severe', label: '严重' },
];

export default function Events() {
  const [data, setData] = useState<EventsPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Filters
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({ category: '', severity: '', studentId: '' });
  const [pendingFilters, setPendingFilters] = useState({ category: '', severity: '', studentId: '' });
  const [ontology, setOntology] = useState<OntologyNode[]>([]);

  // New event form
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ studentId: 'SGPF0001', description: BEHAVIOR_TEMPLATES[0].desc, severity: 'moderate', location: '教室' });
  const [submitting, setSubmitting] = useState(false);
  const [submitResult, setSubmitResult] = useState('');

  // Expanded event detail
  const [expandedEvent, setExpandedEvent] = useState<string | null>(null);
  const [eventDetail, setEventDetail] = useState<ActionEvent | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  // Evaluation
  const [evalForm, setEvalForm] = useState({ effectiveness: 0.7, cost: 0.3, satisfaction: 0.8 });
  const [evalSubmitting, setEvalSubmitting] = useState(false);

  useEffect(() => { api.getOntology().then(setOntology).catch(() => {}); }, []);
  useEffect(() => { loadEvents(); }, [page, filters]);

  const loadEvents = async () => {
    setLoading(true);
    setError('');
    try {
      const d = await api.getEvents({ page, size: 20, ...filters });
      setData(d);
    } catch {
      setError('无法加载事件数据');
    }
    setLoading(false);
  };

  const applyFilters = () => {
    setFilters({ ...pendingFilters });
    setPage(0);
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    setSubmitResult('');
    try {
      const res = await api.processEvent(form);
      setSubmitResult(`事件已提交 → 分类: ${res.classifiedLabel || '—'} | 方案: ${res.matchedIntervention || '—'}`);
      setPage(0);
      await loadEvents();
    } catch (e: any) {
      setSubmitResult('提交失败: ' + (e.message || ''));
    }
    setSubmitting(false);
  };

  const toggleExpand = async (eventId: string) => {
    if (expandedEvent === eventId) {
      setExpandedEvent(null);
      setEventDetail(null);
      return;
    }
    setExpandedEvent(eventId);
    setDetailLoading(true);
    try {
      const detail = await api.getEventDetail(eventId);
      setEventDetail(detail);
    } catch {
      setEventDetail(null);
    }
    setDetailLoading(false);
  };

  const handleEvaluation = async () => {
    if (!eventDetail || !eventDetail.interventionIri) return;
    setEvalSubmitting(true);
    try {
      await api.submitEvaluation({
        interventionIri: eventDetail.interventionIri,
        eventId: eventDetail.eventId,
        studentId: eventDetail.studentId,
        ...evalForm,
      });
      // 从后端重新获取详情，获取准确的 trial 计数、评分等状态
      const detail = await api.getEventDetail(eventDetail.eventId);
      setEventDetail(detail);
      // 用后端数据更新列表中的评价状态
      setData(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          content: prev.content.map(e =>
            e.eventId === detail.eventId
              ? { ...e, hasEvaluation: true, evalEffectiveness: detail.evalEffectiveness ?? evalForm.effectiveness, evalCost: detail.evalCost ?? evalForm.cost, evalSatisfaction: detail.evalSatisfaction ?? evalForm.satisfaction }
              : e
          )
        };
      });
    } catch (e: any) {
      setError('评价提交失败: ' + (e.message || ''));
    }
    setEvalSubmitting(false);
  };

  const severityBadge = (s: string) => {
    const map: Record<string, string> = { mild: 'badge-success', moderate: 'badge-warning', severe: 'badge-danger' };
    const labels: Record<string, string> = { mild: '轻微', moderate: '中等', severe: '严重' };
    return <span className={`badge ${map[s] || ''}`}>{labels[s] || s}</span>;
  };

  const flatOntologyOptions = useCallback((nodes: OntologyNode[], depth = 0): { iri: string; label: string }[] => {
    let result: { iri: string; label: string }[] = [];
    for (const n of nodes) {
      result.push({ iri: n.iri, label: (depth > 0 ? '  '.repeat(depth) : '') + n.label });
      if (n.children) result = result.concat(flatOntologyOptions(n.children, depth + 1));
    }
    return result;
  }, []);

  const categoryOptions = flatOntologyOptions(ontology);

  return (
    <div>
      <div className="page-header">
        <h2>事件追溯</h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginTop: 4 }}>
          浏览行为事件历史，追溯完整处理链路：事件 → 分类 → 方案 → 评价
        </p>
      </div>

      {/* New Event Form (collapsible) */}
      <div className="card mb-4">
        <div className="card-header" onClick={() => setShowForm(!showForm)} style={{ cursor: 'pointer', marginBottom: showForm ? 16 : 0 }}>
          <div className="card-title">{showForm ? '📝 新建事件' : '📝 新建事件（点击展开）'}</div>
          <span style={{ color: 'var(--text-muted)', fontSize: 12 }}>{showForm ? '▲ 收起' : '▼ 展开'}</span>
        </div>
        {showForm && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div>
                <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>学生 ID</label>
                <input type="text" value={form.studentId}
                  onChange={e => setForm(f => ({ ...f, studentId: e.target.value }))}
                  style={inputStyle} />
              </div>
              <div>
                <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>严重程度</label>
                <select value={form.severity}
                  onChange={e => setForm(f => ({ ...f, severity: e.target.value }))}
                  style={inputStyle}>
                  <option value="mild">轻微</option>
                  <option value="moderate">中等</option>
                  <option value="severe">严重</option>
                </select>
              </div>
            </div>
            <div>
              <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>行为描述</label>
              <textarea value={form.description}
                onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                rows={3} style={{ ...inputStyle, resize: 'vertical' }} />
            </div>
            <div className="flex gap-2 flex-wrap">
              {BEHAVIOR_TEMPLATES.map((t, i) => (
                <button key={i} className="btn btn-sm"
                  style={{ background: 'var(--bg-card-hover)', color: 'var(--text-secondary)' }}
                  onClick={() => setForm(f => ({ ...f, description: t.desc, severity: t.sev, location: t.loc }))}>
                  {t.desc.slice(0, 14)}...
                </button>
              ))}
            </div>
            <button className="btn btn-primary" onClick={handleSubmit} disabled={submitting} style={{ alignSelf: 'flex-start' }}>
              {submitting ? '处理中...' : '提交事件'}
            </button>
            {submitResult && (
              <div style={{ fontSize: 13, color: submitResult.startsWith('提交失败') ? 'var(--danger)' : 'var(--success)', padding: '8px 12px', background: 'var(--bg)', borderRadius: 6 }}>
                {submitResult}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Filter Bar */}
      <div className="card mb-4">
        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div style={{ flex: 1, minWidth: 180 }}>
            <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>概念类别</label>
            <select value={pendingFilters.category}
              onChange={e => setPendingFilters(f => ({ ...f, category: e.target.value }))}
              style={inputStyle}>
              <option value="">全部类别</option>
              {categoryOptions.map(c => (
                <option key={c.iri} value={c.iri}>{c.label}</option>
              ))}
            </select>
          </div>
          <div style={{ width: 140 }}>
            <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>严重程度</label>
            <select value={pendingFilters.severity}
              onChange={e => setPendingFilters(f => ({ ...f, severity: e.target.value }))}
              style={inputStyle}>
              {SEVERITY_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </div>
          <div style={{ width: 160 }}>
            <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>学生 ID</label>
            <input type="text" value={pendingFilters.studentId}
              onChange={e => setPendingFilters(f => ({ ...f, studentId: e.target.value }))}
              placeholder="搜索学生..."
              style={inputStyle} />
          </div>
          <button className="btn btn-primary" onClick={applyFilters}>筛选</button>
          {(filters.category || filters.severity || filters.studentId) && (
            <button className="btn" onClick={() => { setPendingFilters({ category: '', severity: '', studentId: '' }); setFilters({ category: '', severity: '', studentId: '' }); setPage(0); }}>
              清除筛选
            </button>
          )}
        </div>
      </div>

      {/* Error */}
      {error && (
        <div className="card mb-4" style={{ color: 'var(--danger)', textAlign: 'center', padding: 16 }}>{error}</div>
      )}

      {/* Events Table */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {loading ? (
          <div className="loading">加载中...</div>
        ) : !data || data.content.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 60, color: 'var(--text-muted)' }}>
            <div style={{ fontSize: 48, marginBottom: 12 }}>📭</div>
            <p>暂无事件记录</p>
            <p style={{ fontSize: 13, marginTop: 8 }}>点击上方"新建事件"提交第一条行为事件</p>
          </div>
        ) : (
          <>
            <div style={{ overflowX: 'auto' }}>
              <table>
                <thead>
                  <tr>
                    <th style={{ width: 40 }}></th>
                    <th>学生</th>
                    <th>事件描述</th>
                    <th>严重度</th>
                    <th>分类结果</th>
                    <th>匹配方案</th>
                    <th>评价</th>
                    <th style={{ width: 140 }}>时间</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((e) => {
                    const isExpanded = expandedEvent === e.eventId;
                    return (
                      <>
                        <tr key={e.eventId}
                          onClick={() => toggleExpand(e.eventId)}
                          style={{ cursor: 'pointer', background: isExpanded ? 'rgba(57,81,65,0.04)' : undefined }}>
                          <td style={{ textAlign: 'center', color: 'var(--text-muted)', fontSize: 10 }}>
                            {isExpanded ? '▼' : '▶'}
                          </td>
                          <td><code style={{ fontSize: 11 }}>{e.studentId}</code></td>
                          <td style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 13 }}>
                            {e.description?.length > 30 ? e.description.slice(0, 30) + '...' : e.description}
                          </td>
                          <td>{severityBadge(e.severity)}</td>
                          <td>
                            <span className="badge badge-info" style={{ fontSize: 11 }}>
                              {e.classifiedLabel || e.classifiedConcept?.split('#')[1] || '—'}
                            </span>
                          </td>
                          <td style={{ fontSize: 13, maxWidth: 150, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {e.matchedIntervention || '—'}
                          </td>
                          <td style={{ textAlign: 'center' }}>
                            {e.hasEvaluation ? <span style={{ fontSize: 12, color: 'var(--success)' }}>✅</span> : <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>⏳</span>}
                          </td>
                          <td style={{ fontSize: 11, color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                            {e.timestamp ? new Date(e.timestamp).toLocaleString('zh-CN') : '—'}
                          </td>
                        </tr>
                        {/* Expanded detail row */}
                        {isExpanded && (
                          <tr key={`${e.eventId}-detail`}>
                            <td colSpan={8} style={{ padding: '0 20px 20px', background: 'rgba(57,81,65,0.02)' }}>
                              {detailLoading ? (
                                <div className="loading" style={{ padding: 20 }}>加载详情...</div>
                              ) : eventDetail ? (
                                <EventDetailChain event={eventDetail} evalForm={evalForm} setEvalForm={setEvalForm}
                                  evalSubmitting={evalSubmitting} onEvaluate={handleEvaluation} />
                              ) : (
                                <EventDetailSummary event={e} evalForm={evalForm} setEvalForm={setEvalForm}
                                  evalSubmitting={evalSubmitting} onEvaluate={handleEvaluation} />
                              )}
                            </td>
                          </tr>
                        )}
                      </>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {data.totalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 20px', borderTop: '1px solid var(--border-light)' }}>
                <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                  共 {data.totalElements} 条，第 {data.page + 1}/{data.totalPages} 页
                </span>
                <div style={{ display: 'flex', gap: 8 }}>
                  <button className="btn btn-sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>上一页</button>
                  <button className="btn btn-sm" disabled={page >= data.totalPages - 1} onClick={() => setPage(p => p + 1)}>下一页</button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

/* Event Detail Chain */
function EventDetailChain({ event, evalForm, setEvalForm, evalSubmitting, onEvaluate }: {
  event: ActionEvent;
  evalForm: { effectiveness: number; cost: number; satisfaction: number };
  setEvalForm: (f: any) => void;
  evalSubmitting: boolean;
  onEvaluate: () => void;
}) {
  const detail = event.interventionDetail;

  return (
    <div className="event-chain">
      <div className="event-chain-title">📋 完整处理链路</div>

      {/* Step 1: Event */}
      <div className="event-chain-step">
        <div className="event-chain-step-header">
          <span className="event-chain-step-num">1</span>
          <span style={{ fontWeight: 600, fontSize: 14 }}>事件信息</span>
        </div>
        <div className="event-chain-step-body">
          <div><span className="event-chain-label">学生</span>{event.studentId}</div>
          <div><span className="event-chain-label">严重程度</span>
            <span className={`badge ${event.severity === 'severe' ? 'badge-danger' : event.severity === 'moderate' ? 'badge-warning' : 'badge-success'}`}>
              {event.severity === 'severe' ? '严重' : event.severity === 'moderate' ? '中等' : '轻微'}
            </span>
          </div>
          <div><span className="event-chain-label">地点</span>{event.location || '—'}</div>
          <div><span className="event-chain-label">描述</span>{event.description}</div>
        </div>
      </div>

      {/* Step 2: Classification */}
      <div className="event-chain-arrow">↓</div>
      <div className="event-chain-step">
        <div className="event-chain-step-header">
          <span className="event-chain-step-num">2</span>
          <span style={{ fontWeight: 600, fontSize: 14 }}>LLM 分类</span>
        </div>
        <div className="event-chain-step-body">
          <div><span className="event-chain-label">匹配概念</span>
            <code style={{ fontSize: 12, background: 'var(--bg)', padding: '2px 8px', borderRadius: 4 }}>
              {event.classifiedConcept?.split('#')[1] || event.classifiedConcept || '—'}
            </code>
          </div>
          <div><span className="event-chain-label">概念标签</span>{event.classifiedLabel || '—'}</div>
        </div>
      </div>

      {/* Step 3: Intervention */}
      <div className="event-chain-arrow">↓</div>
      <div className="event-chain-step">
        <div className="event-chain-step-header">
          <span className="event-chain-step-num">3</span>
          <span style={{ fontWeight: 600, fontSize: 14 }}>干预方案</span>
          {detail && <span className="badge" style={{ marginLeft: 8, fontSize: 10, background: 'var(--primary)', color: '#fff' }}>{detail.generation ? `Gen ${detail.generation}` : ''}</span>}
        </div>
        <div className="event-chain-step-body">
          <div><span className="event-chain-label">方案名称</span>{event.matchedIntervention || detail?.name || '—'}</div>
          {detail?.description && (
            <div><span className="event-chain-label">方案描述</span>{detail.description}</div>
          )}
          {detail?.scoreVector && detail.scoreVector.length > 0 && (
            <div><span className="event-chain-label">三维评分</span>
              <span style={{ fontSize: 12 }}>效果 {detail.scoreVector[0]?.toFixed(2)} | 成本 {detail.scoreVector[1]?.toFixed(2)} | 满意 {detail.scoreVector[2]?.toFixed(2)}</span>
            </div>
          )}
          {detail?.trials !== undefined && (
            <div><span className="event-chain-label">历史试验</span>{detail.trials} 次</div>
          )}
          {detail?.steps && detail.steps.length > 0 && (
            <div>
              <span className="event-chain-label">执行步骤</span>
              <ol style={{ margin: '4px 0 0', paddingLeft: 20, fontSize: 12, color: 'var(--text-secondary)' }}>
                {detail.steps.map((s, i) => <li key={i} style={{ marginBottom: 2 }}>{s}</li>)}
              </ol>
            </div>
          )}
        </div>
      </div>

      {/* Step 4: Evaluation */}
      <div className="event-chain-arrow">↓</div>
      <div className="event-chain-step">
        <div className="event-chain-step-header">
          <span className="event-chain-step-num">4</span>
          <span style={{ fontWeight: 600, fontSize: 14 }}>效果评价</span>
          {event.hasEvaluation && <span className="badge badge-success" style={{ marginLeft: 8, fontSize: 10 }}>已评价</span>}
        </div>
        {event.hasEvaluation ? (
          <div className="event-chain-step-body">
            <div><span className="event-chain-label">有效性</span>{(event.evalEffectiveness ?? 0).toFixed(2)}</div>
            <div><span className="event-chain-label">成本</span>{(event.evalCost ?? 0).toFixed(2)}</div>
            <div><span className="event-chain-label">满意度</span>{(event.evalSatisfaction ?? 0).toFixed(2)}</div>
          </div>
        ) : (
          <div className="event-chain-step-body">
            <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 12 }}>尚未评价，请提交效果反馈</p>
            {[
              { key: 'effectiveness', label: '有效性', desc: '干预是否有效改善了行为' },
              { key: 'cost', label: '成本', desc: '执行方案的资源消耗（越低越好）' },
              { key: 'satisfaction', label: '满意度', desc: '学生和教师对方案的接受度' },
            ].map(item => (
              <div key={item.key} style={{ marginBottom: 10 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                  <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{item.label}</span>
                  <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--primary)' }}>
                    {((evalForm as any)[item.key] * 100).toFixed(0)}%
                  </span>
                </div>
                <input type="range" min="0" max="1" step="0.05"
                  value={(evalForm as any)[item.key]}
                  onChange={e => setEvalForm((f: any) => ({ ...f, [item.key]: parseFloat(e.target.value) }))}
                  style={{ width: '100%', accentColor: 'var(--primary)' }} />
              </div>
            ))}
            <button className="btn btn-primary btn-sm" onClick={onEvaluate} disabled={evalSubmitting}>
              {evalSubmitting ? '提交中...' : '提交评价'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

/* Fallback detail when eventDetail API isn't called */
function EventDetailSummary({ event, evalForm, setEvalForm, evalSubmitting, onEvaluate }: {
  event: ActionEvent;
  evalForm: { effectiveness: number; cost: number; satisfaction: number };
  setEvalForm: (f: any) => void;
  evalSubmitting: boolean;
  onEvaluate: () => void;
}) {
  return (
    <div className="event-chain">
      <div className="event-chain-title">📋 事件概览</div>
      <div className="event-chain-step">
        <div className="event-chain-step-body">
          <div><span className="event-chain-label">学生</span>{event.studentId}</div>
          <div><span className="event-chain-label">严重程度</span>
            <span className={`badge ${event.severity === 'severe' ? 'badge-danger' : event.severity === 'moderate' ? 'badge-warning' : 'badge-success'}`}>
              {event.severity === 'severe' ? '严重' : event.severity === 'moderate' ? '中等' : '轻微'}
            </span>
          </div>
          <div><span className="event-chain-label">描述</span>{event.description}</div>
          <div><span className="event-chain-label">分类</span>
            <code style={{ fontSize: 12, background: 'var(--bg)', padding: '2px 8px', borderRadius: 4 }}>
              {event.classifiedConcept?.split('#')[1] || event.classifiedConcept || '—'}
            </code>
          </div>
          <div><span className="event-chain-label">匹配方案</span>{event.matchedIntervention || '—'}</div>
          <div><span className="event-chain-label">评价状态</span>
            {event.hasEvaluation
              ? <span style={{ color: 'var(--success)', fontSize: 12 }}>已评价 (效果: {event.evalEffectiveness?.toFixed(2)}, 成本: {event.evalCost?.toFixed(2)}, 满意: {event.evalSatisfaction?.toFixed(2)})</span>
              : <span style={{ color: 'var(--text-muted)', fontSize: 12 }}>待评价</span>}
          </div>
        </div>
      </div>
      {!event.hasEvaluation && (
        <>
          <div className="event-chain-arrow">↓</div>
          <div className="event-chain-step">
            <div className="event-chain-step-header">
              <span className="event-chain-step-num">?</span>
              <span style={{ fontWeight: 600, fontSize: 14 }}>提交评价</span>
            </div>
            <div className="event-chain-step-body">
              {[
                { key: 'effectiveness', label: '有效性' },
                { key: 'cost', label: '成本' },
                { key: 'satisfaction', label: '满意度' },
              ].map(item => (
                <div key={item.key} style={{ marginBottom: 10 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                    <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{item.label}</span>
                    <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--primary)' }}>
                      {((evalForm as any)[item.key] * 100).toFixed(0)}%
                    </span>
                  </div>
                  <input type="range" min="0" max="1" step="0.05"
                    value={(evalForm as any)[item.key]}
                    onChange={e => setEvalForm((f: any) => ({ ...f, [item.key]: parseFloat(e.target.value) }))}
                    style={{ width: '100%', accentColor: 'var(--primary)' }} />
                </div>
              ))}
              <button className="btn btn-primary btn-sm" onClick={onEvaluate} disabled={evalSubmitting}>
                {evalSubmitting ? '提交中...' : '提交评价'}
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '8px 12px', borderRadius: 6,
  border: '1px solid var(--border)', background: 'var(--bg-card)',
  color: 'var(--text)', fontSize: 13,
};
