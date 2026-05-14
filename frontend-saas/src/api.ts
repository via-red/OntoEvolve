const BASE = '/api'

async function fetchJSON<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(BASE + url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    throw new Error(`API error: ${res.status} ${res.statusText}`)
  }
  return res.json()
}

// ---- Admin APIs ----
export const adminApi = {
  listDomains: () => fetchJSON<DomainSummary[]>('/admin/domains'),
  getDomain: (id: string) => fetchJSON<DomainDetail>(`/admin/domains/${id}`),
  createDomain: () => fetchJSON<DomainDetail>('/admin/domains', { method: 'POST' }),
  deleteDomain: (id: string) => fetchJSON<void>(`/admin/domains/${id}`, { method: 'DELETE' }),
  publishDomain: (id: string) => fetchJSON<void>(`/admin/domains/${id}/publish`, { method: 'POST' }),
  unpublishDomain: (id: string) => fetchJSON<void>(`/admin/domains/${id}/unpublish`, { method: 'POST' }),

  updateOntology: (id: string, data: unknown) =>
    fetchJSON<void>(`/admin/domains/${id}/ontology`, { method: 'PUT', body: JSON.stringify(data) }),
  updateDecisions: (id: string, data: unknown) =>
    fetchJSON<void>(`/admin/domains/${id}/decisions`, { method: 'PUT', body: JSON.stringify(data) }),
  updateFeedback: (id: string, data: unknown) =>
    fetchJSON<void>(`/admin/domains/${id}/feedback`, { method: 'PUT', body: JSON.stringify(data) }),
  updateRules: (id: string, data: unknown) =>
    fetchJSON<void>(`/admin/domains/${id}/rules`, { method: 'PUT', body: JSON.stringify(data) }),
  updateFields: (id: string, data: unknown) =>
    fetchJSON<void>(`/admin/domains/${id}/fields`, { method: 'PUT', body: JSON.stringify(data) }),
}

// ---- Domain workspace APIs ----
export function domainApi(domainId: string) {
  const prefix = `/${domainId}`
  return {
    getOntology: () => fetchJSON<OntologyNode[]>(`${prefix}/ontology`),
    submitRecord: (data: Record<string, unknown>) =>
      fetchJSON<RecordResult>(`${prefix}/records`, { method: 'POST', body: JSON.stringify(data) }),
    listRecords: (params?: Record<string, string>) => {
      const qs = params ? '?' + new URLSearchParams(params).toString() : ''
      return fetchJSON<RecordListItem[]>(`${prefix}/records${qs}`)
    },
    getRecord: (id: string) => fetchJSON<RecordDetail>(`${prefix}/records/${id}`),
    submitFeedback: (recordId: string, scores: number[]) =>
      fetchJSON<void>(`${prefix}/records/${recordId}/feedback`, {
        method: 'POST',
        body: JSON.stringify({ scores }),
      }),
    getPopulations: () => fetchJSON<PopulationView[]>(`${prefix}/populations`),
    triggerEvolution: (conceptIri: string) =>
      fetchJSON<void>(`${prefix}/evolve`, {
        method: 'POST',
        body: JSON.stringify({ conceptIri }),
      }),
    getGraph: (conceptIri?: string) => {
      const qs = conceptIri ? `?conceptIri=${encodeURIComponent(conceptIri)}` : ''
      return fetchJSON<GraphData>(`${prefix}/graph${qs}`)
    },
    getMetrics: () => fetchJSON<MetricsData>(`${prefix}/metrics`),
  }
}

// ---- Types ----

export interface DomainSummary {
  id: string
  name: string
  description: string
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
}

export interface DomainDetail extends DomainSummary {
  namespace: string
  llm: { provider: string; model: string }
  store: { type: string; config: Record<string, unknown> }
}

export interface OntologyNode {
  iri: string
  label: string
  children: OntologyNode[]
  properties?: Record<string, unknown>
}

export interface RecordResult {
  id: string
  classifiedConcept: string
  classifiedLabel: string
  matchedDecision: string | null
  matchedDecisionName: string | null
  [key: string]: unknown
}

export interface RecordListItem {
  id: string
  classifiedConcept: string
  matchedDecision: string | null
  hasFeedback: boolean
  timestamp: string
  [key: string]: unknown
}

export interface RecordDetail extends RecordResult {
  scores: number[] | null
  candidates: AssignmentView[]
  [key: string]: unknown
}

export interface AssignmentView {
  iri: string
  decisionName: string
  scoreVector: number[]
  trials: number
  generation: number
  status: string
}

export interface PopulationView {
  conceptIri: string
  conceptLabel: string
  size: number
  generation: number
  eliteCount: number
}

export interface GraphNode {
  id: string
  type: string
  label: string
}

export interface GraphEdge {
  source: string
  target: string
  type: string
}

export interface GraphData {
  conceptIri: string
  conceptLabel: string
  nodes: GraphNode[]
  edges: GraphEdge[]
}

export interface MetricsData {
  totalRecords: number
  totalFeedbacks: number
  nicheCount: number
  llmCalls: number
}
