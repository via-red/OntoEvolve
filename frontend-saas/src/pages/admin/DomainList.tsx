import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminApi, DomainSummary } from '../../api'

export default function DomainList() {
  const [domains, setDomains] = useState<DomainSummary[]>([])

  const load = () => adminApi.listDomains().then(setDomains).catch(() => {})

  useEffect(() => { load() }, [])

  const handleCreate = async () => {
    await adminApi.createDomain()
    load()
  }

  const handleToggle = async (d: DomainSummary) => {
    if (d.status === 'PUBLISHED') {
      await adminApi.unpublishDomain(d.id)
    } else {
      await adminApi.publishDomain(d.id)
    }
    load()
  }

  const handleDelete = async (id: string) => {
    if (!confirm('确定删除该领域？')) return
    await adminApi.deleteDomain(id)
    load()
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ margin: 0, fontSize: 20 }}>领域管理</h1>
        <button onClick={handleCreate} style={btnStyle}>新建领域</button>
      </div>

      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ background: '#fafafa' }}>
            <th style={thStyle}>ID</th>
            <th style={thStyle}>名称</th>
            <th style={thStyle}>状态</th>
            <th style={thStyle}>操作</th>
          </tr>
        </thead>
        <tbody>
          {domains.map((d) => (
            <tr key={d.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
              <td style={tdStyle}>{d.id}</td>
              <td style={tdStyle}>{d.name}</td>
              <td style={tdStyle}>
                <span style={{
                  padding: '2px 8px',
                  borderRadius: 4,
                  fontSize: 12,
                  background: d.status === 'PUBLISHED' ? '#f6ffed' : '#fff7e6',
                  color: d.status === 'PUBLISHED' ? '#52c41a' : '#fa8c16',
                }}>
                  {d.status === 'PUBLISHED' ? '已发布' : '草稿'}
                </span>
              </td>
              <td style={tdStyle}>
                <Link to={`/admin/domains/${d.id}`} style={{ marginRight: 12, color: '#1677ff' }}>编辑</Link>
                <button onClick={() => handleToggle(d)} style={linkBtnStyle}>
                  {d.status === 'PUBLISHED' ? '下架' : '发布'}
                </button>
                <button onClick={() => handleDelete(d.id)} style={{ ...linkBtnStyle, color: '#ff4d4f' }}>删除</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

const thStyle: React.CSSProperties = { padding: '12px 16px', textAlign: 'left', fontWeight: 600, fontSize: 13 }
const tdStyle: React.CSSProperties = { padding: '12px 16px', fontSize: 14 }
const btnStyle: React.CSSProperties = { padding: '8px 16px', background: '#1677ff', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer' }
const linkBtnStyle: React.CSSProperties = { background: 'none', border: 'none', color: '#1677ff', cursor: 'pointer', fontSize: 14, padding: 0 }
