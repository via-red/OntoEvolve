const API_BASE = '/api';

async function fetchJSON<T>(url: string): Promise<T> {
  const res = await fetch(`${API_BASE}${url}`);
  if (!res.ok) throw new Error(`API error: ${res.status} ${res.statusText}`);
  return res.json();
}

async function postJSON<T>(url: string, body: any): Promise<T> {
  const res = await fetch(`${API_BASE}${url}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`API error: ${res.status} ${res.statusText}`);
  return res.json();
}

export const api = {
  // Dashboard
  getDashboard: () => fetchJSON<any>('/education/dashboard'),

  // Students
  getStudents: () => fetchJSON<any[]>('/students'),
  getStudent: (id: number) => fetchJSON<any>(`/students/${id}`),
  searchStudents: (q: string) => fetchJSON<any[]>(`/students/search?q=${encodeURIComponent(q)}`),

  // Events
  getEvents: (params?: { page?: number; size?: number; category?: string; severity?: string; studentId?: string }) => {
    const searchParams = new URLSearchParams();
    if (params?.page !== undefined) searchParams.set('page', String(params.page));
    if (params?.size) searchParams.set('size', String(params.size));
    if (params?.category) searchParams.set('category', params.category);
    if (params?.severity) searchParams.set('severity', params.severity);
    if (params?.studentId) searchParams.set('studentId', params.studentId);
    const qs = searchParams.toString();
    return fetchJSON<any>(`/education/events${qs ? '?' + qs : ''}`);
  },
  getEventDetail: (eventId: string) => fetchJSON<any>(`/education/events/${encodeURIComponent(eventId)}`),
  processEvent: (body: any) => postJSON<any>('/education/event', body),
  submitEvaluation: (body: any) => postJSON<any>('/education/evaluation', body),

  // Interventions
  getInterventions: (params?: { conceptIri?: string; status?: string; interventionType?: string; sortBy?: string }) => {
    const searchParams = new URLSearchParams();
    if (params?.conceptIri) searchParams.set('conceptIri', params.conceptIri);
    if (params?.status) searchParams.set('status', params.status);
    if (params?.interventionType) searchParams.set('interventionType', params.interventionType);
    if (params?.sortBy) searchParams.set('sortBy', params.sortBy);
    const qs = searchParams.toString();
    return fetchJSON<any[]>(`/education/interventions${qs ? '?' + qs : ''}`);
  },
  getInterventionLineage: (iri: string) => fetchJSON<any>(`/education/interventions/lineage?iri=${encodeURIComponent(iri)}`),

  // Populations & Evolution
  getPopulations: () => fetchJSON<any[]>('/education/populations'),
  getPopulation: (iri: string) => fetchJSON<any>(`/education/populations?conceptIri=${encodeURIComponent(iri)}`),
  triggerEvolution: (conceptIri: string) => postJSON<any>(`/education/evolve?conceptIri=${encodeURIComponent(conceptIri)}`, {}),

  // Graph
  getGraphData: (conceptIri: string) => fetchJSON<any>(`/education/graph?conceptIri=${encodeURIComponent(conceptIri)}`),
  getOntology: () => fetchJSON<any[]>('/education/ontology'),

  // Traces & Metrics
  getTraces: () => fetchJSON<any[]>('/education/traces'),
  getMetrics: () => fetchJSON<any>('/education/metrics'),
  getLlmMetrics: () => fetchJSON<any>('/education/metrics/llm'),
};
