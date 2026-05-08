export interface Student {
  id: number;
  school: string;
  sex: string;
  age: number;
  address: string;
  studytime: number;
  failures: number;
  absences: number;
  health: number;
  g1: number;
  g2: number;
  g3: number;
  studentId: string;
  [key: string]: any;
}

export interface ActionEvent {
  eventId: string;
  studentId: string;
  description: string;
  location: string;
  severity: string;
  classifiedConcept: string;
  classifiedLabel: string;
  matchedIntervention: string;
  timestamp: string;
  /** Backward compatibility: returned by processEvent endpoint */
  suggestion?: string;
  /** Error message on failure */
  error?: string;
}

export interface OntologyNode {
  iri: string;
  label: string;
  comment: string;
  category: string;
  children: OntologyNode[];
}

export interface Intervention {
  id: number;
  iri: string;
  name: string;
  description: string;
  steps: string;
  interventionType: string;
  conceptIri: string;
  generation: number;
  trials: number;
  scoreEffectiveness: number;
  scoreCost: number;
  scoreSatisfaction: number;
}

export interface PopulationView {
  conceptIri: string;
  conceptLabel: string;
  conceptCategory: string;
  generation: number;
  size: number;
  activeCount: number;
}

export interface EvolTrace {
  type: string;
  timestamp: string;
  decision: string;
  context: string;
}

export interface SystemMetrics {
  totalFeedbacks: number;
  llmCalls: number;
  averageHypervolume: number;
  nicheDiversity: number;
  totalPopulations: number;
  totalEvents: number;
  totalStudents: number;
  totalInterventions: number;
}
