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
  getStudents: () => fetchJSON<any[]>('/students'),
  getStudent: (id: number) => fetchJSON<any>(`/students/${id}`),

  getEvents: () => fetchJSON<any[]>('/education/events'),
  processEvent: (body: any) => postJSON<any>('/education/event', body),
  submitEvaluation: (body: any) => postJSON<string>('/education/evaluation', body),

  getPopulations: () => fetchJSON<any[]>('/education/populations'),
  getPopulation: (iri: string) => fetchJSON<any>(`/education/populations/${encodeURIComponent(iri)}`),
  triggerEvolution: (conceptIri: string) => postJSON<any>(`/education/evolve?conceptIri=${encodeURIComponent(conceptIri)}`, {}),

  getTraces: () => fetchJSON<any[]>('/education/traces'),
  getMetrics: () => fetchJSON<any>('/education/metrics'),

  getOntology: () => fetchJSON<any>('/education/ontology'),
};
