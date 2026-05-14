import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { adminApi, DomainDetail } from '../../api'

export default function DomainEditor() {
  const { domainId } = useParams<{ domainId: string }>()
  const [domain, setDomain] = useState<DomainDetail | null>(null)

  useEffect(() => {
    if (domainId) {
      adminApi.getDomain(domainId).then(setDomain).catch(() => {})
    }
  }, [domainId])

  if (!domain) return <div>加载中...</div>

  return (
    <div style={{ maxWidth: 800 }}>
      <h1 style={{ fontSize: 20, marginBottom: 24 }}>编辑领域: {domain.name}</h1>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <Section title="基本信息">
          <Field label="ID" value={domain.id} />
          <Field label="名称" value={domain.name} />
          <Field label="描述" value={domain.description} />
          <Field label="命名空间" value={domain.namespace} />
          <Field label="状态" value={domain.status} />
        </Section>

        <Section title="LLM 配置">
          <Field label="提供商" value={domain.llm.provider} />
          <Field label="模型" value={domain.llm.model} />
        </Section>

        <Section title="存储配置">
          <Field label="类型" value={domain.store.type} />
        </Section>

        <div style={{ display: 'flex', gap: 12 }}>
          <button style={btnStyle}>保存</button>
          <button style={{ ...btnStyle, background: '#52c41a' }}>
            {domain.status === 'PUBLISHED' ? '更新并发布' : '发布'}
          </button>
        </div>
      </div>
    </div>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div style={{ border: '1px solid #e0e0e0', borderRadius: 8, padding: 16 }}>
      <h3 style={{ margin: '0 0 12px', fontSize: 15 }}>{title}</h3>
      {children}
    </div>
  )
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ marginBottom: 8, display: 'flex' }}>
      <span style={{ width: 120, color: '#666', fontSize: 14 }}>{label}</span>
      <span style={{ fontSize: 14 }}>{value}</span>
    </div>
  )
}

const btnStyle: React.CSSProperties = {
  padding: '8px 20px', background: '#1677ff', color: '#fff',
  border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 14,
}
