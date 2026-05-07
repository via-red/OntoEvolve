import { useEffect, useState } from 'react';
import { api } from '../api';
import type { Student } from '../types';

export default function Students() {
  const [students, setStudents] = useState<Student[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<Student | null>(null);

  useEffect(() => {
    api.getStudents()
      .then(setStudents)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const filtered = students.filter(s =>
    !search || s.studentId?.toLowerCase().includes(search.toLowerCase()) ||
    s.school?.toLowerCase().includes(search.toLowerCase())
  );

  const getGradeLevel = (g3: number) => {
    if (g3 >= 18) return { label: '优秀', color: 'var(--success)' };
    if (g3 >= 14) return { label: '良好', color: 'var(--accent)' };
    if (g3 >= 10) return { label: '及格', color: 'var(--warning)' };
    return { label: '不及格', color: 'var(--danger)' };
  };

  if (loading) return <div className="loading">加载学生数据...</div>;

  return (
    <div>
      <div className="page-header">
        <h2>👨‍🎓 学生数据</h2>
        <p>UCI Student Performance 数据集 — 共 {students.length} 名学生</p>
      </div>

      <div className="flex gap-4 mb-6" style={{ alignItems: 'center' }}>
        <input
          type="text"
          placeholder="搜索学生ID或学校..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{
            flex: 1, padding: '8px 14px', borderRadius: 'var(--radius-sm)',
            border: '1px solid var(--border-light)', background: 'var(--bg-card)',
            color: 'var(--text)', fontSize: 14,
          }}
        />
        <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
          {filtered.length} 条结果
        </span>
      </div>

      <div className="grid grid-2">
        <div className="card" style={{ overflow: 'auto', maxHeight: '70vh' }}>
          <table>
            <thead>
              <tr>
                <th>学生ID</th>
                <th>学校</th>
                <th>性别</th>
                <th>年龄</th>
                <th>缺勤</th>
                <th>G3</th>
                <th>等级</th>
              </tr>
            </thead>
            <tbody>
              {filtered.slice(0, 200).map(s => {
                const grade = getGradeLevel(s.g3);
                return (
                  <tr key={s.id}
                    onClick={() => setSelected(s)}
                    style={{ cursor: 'pointer', background: selected?.id === s.id ? 'rgba(79,70,229,0.08)' : undefined }}
                  >
                    <td><code style={{ fontSize: 12 }}>{s.studentId}</code></td>
                    <td>
                      <span className={`badge ${s.school === 'GP' ? 'badge-info' : 'badge-warning'}`}>
                        {s.school}
                      </span>
                    </td>
                    <td>{s.sex === 'F' ? '女' : '男'}</td>
                    <td>{s.age}</td>
                    <td>
                      <span style={{ color: s.absences > 10 ? 'var(--danger)' : 'inherit' }}>
                        {s.absences}
                      </span>
                    </td>
                    <td style={{ fontWeight: 600 }}>{s.g3}</td>
                    <td><span className="badge" style={{ background: `${grade.color}20`, color: grade.color }}>{grade.label}</span></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {filtered.length > 200 && (
            <div style={{ textAlign: 'center', padding: 12, color: 'var(--text-muted)', fontSize: 13 }}>
              显示前 200 条，共 {filtered.length} 条
            </div>
          )}
        </div>

        <div>
          {selected ? (
            <div className="card">
              <div className="card-header">
                <div className="card-title">学生详情 — {selected.studentId}</div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, fontSize: 14 }}>
                {[
                  { label: '学校', value: selected.school, badge: true },
                  { label: '性别', value: selected.sex === 'F' ? '女' : '男' },
                  { label: '年龄', value: selected.age },
                  { label: '住址', value: selected.address === 'U' ? '城市' : '农村' },
                  { label: '学习时间', value: ['<2h', '2-5h', '5-10h', '>10h'][selected.studytime - 1] || selected.studytime },
                  { label: '挂科次数', value: selected.failures, danger: selected.failures > 2 },
                  { label: '缺勤次数', value: selected.absences, danger: selected.absences > 10 },
                  { label: '健康状态', value: ['很差', '较差', '一般', '良好', '优秀'][selected.health - 1] || selected.health },
                  { label: 'G1 成绩', value: selected.g1 },
                  { label: 'G2 成绩', value: selected.g2 },
                  { label: 'G3 成绩', value: selected.g3 },
                  { label: '课外活动', value: selected.activities === 'yes' ? '参加' : '未参加' },
                ].map((item, i) => (
                  <div key={i} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border-light)' }}>
                    <span style={{ color: 'var(--text-secondary)' }}>{item.label}</span>
                    <span style={{
                      fontWeight: 600,
                      color: (item as any).danger ? 'var(--danger)' : 'inherit',
                    }}>
                      {(item as any).badge ? <span className={`badge ${selected.school === 'GP' ? 'badge-info' : 'badge-warning'}`}>{item.value}</span> : item.value}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 200, color: 'var(--text-muted)' }}>
              点击左侧学生查看详情
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
