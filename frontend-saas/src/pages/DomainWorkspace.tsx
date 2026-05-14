import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { domainApi, OntologyNode, RecordResult } from '../api'

export default function DomainWorkspace() {
  const { domainId } = useParams<{ domainId: string }>()
  const [ontology, setOntology] = useState<OntologyNode[]>([])
  const [lastResult, setLastResult] = useState<RecordResult | null>(null)

  const api = domainApi(domainId!)

  useEffect(() => {
    api.getOntology().then(setOntology).catch(() => {})
  }, [domainId])

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const data = Object.fromEntries(new FormData(e.currentTarget)) as Record<string, unknown>
    const result = await api.submitRecord(data)
    setLastResult(result)
  }

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: 20 }}>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 24 }}>
        <Link to="/" style={{ color: '#1677ff' }}>← 返回</Link>
        <h1 style={{ margin: 0 }}>领域: {domainId}</h1>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        {/* 左栏: 提交记录 */}
        <div>
          <h2>提交记录</h2>
          <form onSubmit={handleSubmit}>
            <div style={{ marginBottom: 12 }}>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>描述</label>
              <textarea name="description" required style={inputStyle} rows={3} />
            </div>
            <div style={{ marginBottom: 12 }}>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>分类字段</label>
              <input name="extraField" placeholder="其他字段" style={inputStyle} />
            </div>
            <button type="submit" style={btnStyle}>提交</button>
          </form>

          {lastResult && (
            <div style={{ marginTop: 16, padding: 12, background: '#f6ffed', borderRadius: 6 }}>
              <h3>分类结果</h3>
              <p>概念: {lastResult.classifiedLabel}</p>
              <p>匹配决策: {lastResult.matchedDecisionName ?? '无'}</p>
            </div>
          )}
        </div>

        {/* 右栏: 概念树 */}
        <div>
          <h2>概念树</h2>
          <OntologyTree nodes={ontology} />
        </div>
      </div>
    </div>
  )
}

function OntologyTree({ nodes, depth = 0 }: { nodes: OntologyNode[]; depth?: number }) {
  return (
    <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
      {nodes.map((node) => (
        <li key={node.iri} style={{ paddingLeft: depth * 16, marginBottom: 4 }}>
          <span style={{ fontSize: 14 }}>{node.label}</span>
          {node.children && node.children.length > 0 && (
            <OntologyTree nodes={node.children} depth={depth + 1} />
          )}
        </li>
      ))}
    </ul>
  )
}

const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: '8px 12px',
  border: '1px solid #d9d9d9',
  borderRadius: 6,
  fontSize: 14,
}

const btnStyle: React.CSSProperties = {
  padding: '8px 20px',
  background: '#1677ff',
  color: '#fff',
  border: 'none',
  borderRadius: 6,
  cursor: 'pointer',
  fontSize: 14,
}
