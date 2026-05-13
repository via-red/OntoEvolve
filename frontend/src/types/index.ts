// ================================================================
// Dashboard
// ================================================================

export interface DashboardStats {
  todayEvents: number;
  pendingEvaluations: number;
  activeInterventions: number;
  totalGenerations: number;
  totalFeedbacks: number;
  totalPopulations: number;
  totalStudents: number;
  llmCalls: number;
  evolutionRuns: number;
}

export interface RecentActivity {
  eventId: string;
  studentId: string;
  description: string;
  location: string;
  severity: string;
  classifiedConcept: string;
  classifiedLabel: string;
  matchedIntervention: string;
  interventionIri: string;
  timestamp: string;
  hasEvaluation: boolean;
}

export interface TopIntervention {
  iri: string;
  name: string;
  description: string;
  effectiveness: number;
  cost: number;
  satisfaction: number;
  trials: number;
  conceptLabel: string;
}

export interface NicheHealth {
  conceptIri: string;
  conceptLabel: string;
  populationSize: number;
  generation: number;
  avgEffectiveness: number;
  avgCost: number;
  avgSatisfaction: number;
  totalTrials: number;
  eliteCount: number;
}

export interface DashboardData {
  stats: DashboardStats;
  recentActivity: RecentActivity[];
  topInterventions: TopIntervention[];
  nicheHealth: NicheHealth[];
}

// ================================================================
// Events
// ================================================================

export interface ActionEvent {
  eventId: string;
  studentId: string;
  description: string;
  location: string;
  severity: string;
  classifiedConcept: string;
  classifiedLabel: string;
  matchedIntervention: string;
  interventionIri?: string;
  suggestion?: string;
  timestamp: string;
  hasEvaluation?: boolean;
  evalEffectiveness?: number;
  evalCost?: number;
  evalSatisfaction?: number;
  candidates?: AssignmentView[];
  interventionDetail?: AssignmentView;
  error?: string;
}

export interface EventsPage {
  content: ActionEvent[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// ================================================================
// Interventions
// ================================================================

export interface InterventionSummary {
  iri: string;
  decisionIri: string;
  name: string;
  description: string;
  steps: string[];
  interventionType: string;
  requiresParentApproval: boolean;
  scoreVector: number[];
  trials: number;
  generation: number;
  status: string;
  conceptIri: string;
  conceptLabel: string;
  parentNames: string[];
}

export interface LineageNode {
  iri: string;
  name: string;
  generation: number;
  status: string;
}

export interface LineageData {
  iri: string;
  name: string;
  generation: number;
  status: string;
  ancestors: LineageNode[];
  descendants: LineageNode[];
  producedBy: { id: string; type: string; timestamp: string; context: string }[];
}

// ================================================================
// Graph
// ================================================================

export interface GraphNode {
  id: string;
  type: string;
  label: string;
  properties: Record<string, string>;
}

export interface GraphEdge {
  source: string;
  target: string;
  type: string;
}

export interface GraphData {
  conceptIri: string;
  conceptLabel: string;
  nodes: GraphNode[];
  edges: GraphEdge[];
}

// ================================================================
// Ontology
// ================================================================

export interface OntologyNode {
  iri: string;
  label: string;
  comment: string;
  category: string;
  assignmentCount: number;
  eventCount: number;
  children: OntologyNode[];
}

// ================================================================
// Populations & Evolution
// ================================================================

export interface PopulationView {
  conceptIri: string;
  conceptLabel: string;
  conceptCategory: string;
  generation: number;
  size: number;
  activeCount: number;
  eliteCount: number;
  avgEffectiveness: number;
  avgCost: number;
}

export interface AssignmentView {
  iri: string;
  decisionIri: string;
  name: string;
  description: string;
  steps: string[];
  scoreVector: number[];
  trials: number;
  generation: number;
  status: string;
  validationStatus?: string;
  parentDecisionIris: string[];
  parentNames: string[];
  interventionType?: string;
}

export interface PopulationDetail {
  concept: string;
  generation: number;
  size: number;
  activeCount: number;
  members: AssignmentView[];
  paretoPoints?: { effectiveness: number; cost: number; satisfaction: number }[];
}

// ================================================================
// Traces & Metrics
// ================================================================

export interface EvolTrace {
  type: string;
  timestamp: string;
  decision: string;
  decisionIri?: string;
  context: string;
  parents?: { name: string; iri: string }[];
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

export interface LlmMetrics {
  totalCalls: number;
  totalPromptTokens: number;
  totalCompletionTokens: number;
  averageLatencyMs: number;
  errorCount: number;
}

// ================================================================
// Student
// ================================================================

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
