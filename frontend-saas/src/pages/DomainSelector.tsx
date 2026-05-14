import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminApi, DomainSummary } from '../api'

export default function DomainSelector() {
  const [domains, setDomains] = useState<DomainSummary[]>([])

  useEffect(() => {
    adminApi.listDomains().then(setDomains).catch(() => {})
  }, [])

  return (
    <div style={{ maxWidth: 800, margin: '60px auto', padding: '0 20px' }}>
      <h1>OntoEvolve SaaS</h1>
      <p style={{ color: '#666', marginBottom: 32 }}>
        选择一个领域开始操作，或进入管理后台配置领域。
      </p>

      <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
        <Link to="/admin" style={btnStyle}>
          领域管理
        </Link>
      </div>

      <h2>已发布领域</h2>
      {domains.length === 0 && <p style={{ color: '#999' }}>暂无已发布的领域</p>}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {domains
          .filter((d) => d.status === 'PUBLISHED')
          .map((d) => (
            <Link
              key={d.id}
              to={`/${d.id}`}
              style={{
                display: 'block',
                padding: 16,
                border: '1px solid #e0e0e0',
                borderRadius: 8,
                textDecoration: 'none',
                color: 'inherit',
              }}
            >
              <strong>{d.name}</strong>
              <p style={{ margin: '4px 0 0', color: '#666', fontSize: 14 }}>{d.description}</p>
            </Link>
          ))}
      </div>
    </div>
  )
}

const btnStyle: React.CSSProperties = {
  display: 'inline-block',
  padding: '8px 16px',
  background: '#1677ff',
  color: '#fff',
  borderRadius: 6,
  textDecoration: 'none',
  fontSize: 14,
}
