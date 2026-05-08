package com.ontoevolve.domain.education.controller;

import com.ontoevolve.core.kernel.DecisionPopulation;
import com.ontoevolve.core.kernel.EvolutionEngine;
import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Execution;
import com.ontoevolve.core.model.Feedback;
import com.ontoevolve.core.validation.OntologyValidator;
import com.ontoevolve.graphstore.node.ActionEventNode;
import com.ontoevolve.graphstore.node.ActionTypeNode;
import com.ontoevolve.graphstore.node.AssignmentNode;
import com.ontoevolve.graphstore.node.EvaluationNode;
import com.ontoevolve.graphstore.node.ExecutionNode;
import com.ontoevolve.graphstore.repository.ActionEventRepository;
import com.ontoevolve.graphstore.repository.ActionTypeRepository;
import com.ontoevolve.graphstore.repository.EvaluationRepository;
import com.ontoevolve.graphstore.repository.ExecutionRepository;
import com.ontoevolve.graphstore.repository.AssignmentRepository;
import com.ontoevolve.domain.education.model.ActionEvent;
import com.ontoevolve.domain.education.model.ActionType;
import com.ontoevolve.domain.education.model.Intervention;
import com.ontoevolve.domain.education.repository.StudentRepository;
import com.ontoevolve.domain.education.service.InterventionService;
import com.ontoevolve.infra.metrics.MetricsCollector;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/education")
public class EventController {

    private final InterventionService interventionService;
    private final MetricsCollector metrics;
    private final StudentRepository studentRepo;
    private final OntologyValidator ontologyValidator;

    // Optional Neo4j repositories — null when onto.graph.store-type=memory
    private final ActionEventRepository actionEventNeoRepo;
    private final ActionTypeRepository actionTypeNeoRepo;
    private final EvaluationRepository evaluationNeoRepo;
    private final ExecutionRepository executionNeoRepo;
    private final AssignmentRepository assignmentNeoRepo;

    // In-memory event store for memory mode
    private final List<Map<String, Object>> memoryEventStore = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong memoryEventCount = new AtomicLong(0);
    private final AtomicLong memoryInterventionCount = new AtomicLong(0);

    public EventController(InterventionService interventionService,
                           MetricsCollector metrics,
                           StudentRepository studentRepo,
                           OntologyValidator ontologyValidator,
                           ObjectProvider<ActionEventRepository> actionEventNeoRepo,
                           ObjectProvider<ActionTypeRepository> actionTypeNeoRepo,
                           ObjectProvider<EvaluationRepository> evaluationNeoRepo,
                           ObjectProvider<ExecutionRepository> executionNeoRepo,
                           ObjectProvider<AssignmentRepository> assignmentNeoRepo) {
        this.interventionService = interventionService;
        this.metrics = metrics;
        this.studentRepo = studentRepo;
        this.ontologyValidator = ontologyValidator;
        this.actionEventNeoRepo = actionEventNeoRepo.getIfAvailable();
        this.actionTypeNeoRepo = actionTypeNeoRepo.getIfAvailable();
        this.evaluationNeoRepo = evaluationNeoRepo.getIfAvailable();
        this.executionNeoRepo = executionNeoRepo.getIfAvailable();
        this.assignmentNeoRepo = assignmentNeoRepo.getIfAvailable();
    }

    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> handleEvent(@RequestBody Map<String, String> body) {
        ActionEvent event = new ActionEvent(
                "event:" + UUID.randomUUID(),
                Instant.now(),
                body.get("description"),
                body.get("studentId"),
                body.getOrDefault("location", ""),
                body.getOrDefault("severity", "medium")
        );

        Intervention suggestion = interventionService.processEvent(event);

        // Resolve actual classified concept info
        String conceptIri = "";
        String conceptLabel = "";
        if (suggestion != null) {
            ActionType classifiedType = findConceptForIntervention(suggestion.getIri());
            if (classifiedType != null) {
                conceptIri = classifiedType.getIri();
                conceptLabel = classifiedType.getLabel();
            }
        }

        // Build event record for history
        Map<String, Object> eventRecord = new LinkedHashMap<>();
        eventRecord.put("eventId", event.getId());
        eventRecord.put("studentId", event.getStudentId());
        eventRecord.put("description", event.getBehaviorDescription());
        eventRecord.put("location", event.getLocation());
        eventRecord.put("severity", event.getSeverity());
        eventRecord.put("classifiedConcept", conceptIri);
        eventRecord.put("classifiedLabel", conceptLabel);
        eventRecord.put("matchedIntervention", suggestion != null ? suggestion.getName() : "无匹配方案");
        eventRecord.put("interventionIri", suggestion != null ? suggestion.getIri() : "");
        eventRecord.put("suggestion", suggestion != null ? suggestion.getName() : "无匹配方案");
        eventRecord.put("timestamp", event.getTimestamp().toString());

        // Persist to Neo4j when available
        if (actionEventNeoRepo != null && suggestion != null) {
            persistEventWithRelation(event, suggestion);
        }

        // Add candidate solutions from the population so the user can choose
        if (!conceptIri.isEmpty()) {
            DecisionPopulation pop = interventionService.getEvolutionEngine()
                    .getPopulations().get(conceptIri);
            if (pop != null) {
                List<Map<String, Object>> candidates = pop.getAllMembers().stream()
                        .map(a -> {
                            Map<String, Object> m = new HashMap<>();
                            m.put("iri", a.getIri());
                            m.put("decisionIri", a.getDecision().getIri());
                            m.put("name", a.getDecision().getName());
                            m.put("description", a.getDecision().getDescription());
                            m.put("steps", a.getDecision().getSteps());
                            m.put("scoreVector", a.getScoreVector());
                            m.put("trials", a.getTrials());
                            m.put("generation", a.getGeneration());
                            m.put("status", a.getStatus().name());
                            return m;
                        }).toList();
                eventRecord.put("candidates", candidates);
            }
        }

        // Always store in memory for history display
        memoryEventStore.add(eventRecord);
        memoryEventCount.incrementAndGet();
        if (suggestion != null) memoryInterventionCount.incrementAndGet();

        return ResponseEntity.ok(eventRecord);
    }

    private ActionType findConceptForIntervention(String interventionIri) {
        return interventionService.getEvolutionEngine().getPopulations().entrySet().stream()
                .filter(e -> e.getValue().getActiveMembers().stream()
                        .anyMatch(a -> a.getDecision().getIri().equals(interventionIri)))
                .findFirst()
                .map(e -> {
                    com.ontoevolve.core.model.Concept c = e.getValue().getConcept();
                    if (c instanceof ActionType at) return at;
                    return null;
                })
                .orElse(null);
    }

    private void persistEventWithRelation(ActionEvent event, Intervention suggestion) {
        ActionTypeNode conceptNode = actionTypeNeoRepo.findById(suggestion.getIri())
                .orElseGet(() -> {
                    // Fallback: create a minimal node
                    ActionTypeNode node = new ActionTypeNode(
                            suggestion.getIri(), suggestion.getName(), "", "");
                    return actionTypeNeoRepo.save(node);
                });

        ActionEventNode eventNode = new ActionEventNode(
                event.getId(), event.getStudentId(), event.getBehaviorDescription(),
                event.getLocation(), event.getSeverity(), conceptNode);
        if (suggestion != null) {
            eventNode.setMatchedIntervention(suggestion.getName());
        }
        eventNode.setTimestamp(event.getTimestamp());
        actionEventNeoRepo.save(eventNode);
    }

    @PostMapping("/evaluation")
    public ResponseEntity<Map<String, String>> submitEvaluation(@RequestBody Map<String, String> body) {
        String interventionIri = body.getOrDefault("interventionIri", "");
        String studentId = body.getOrDefault("studentId", "unknown");
        double effectiveness = Double.parseDouble(body.getOrDefault("effectiveness", "0.5"));
        double cost = Double.parseDouble(body.getOrDefault("cost", "0.3"));
        double satisfaction = Double.parseDouble(body.getOrDefault("satisfaction", "0.8"));

        interventionService.submitEvaluationByIntervention(
                interventionIri, studentId, effectiveness, cost, satisfaction
        );

        // Persist evaluation chain to Neo4j when available
        if (evaluationNeoRepo != null && executionNeoRepo != null && assignmentNeoRepo != null) {
            persistEvaluationChain(interventionIri, studentId, effectiveness, cost, satisfaction);
        }

        return ResponseEntity.ok(Map.of("message", "评价已提交"));
    }

    private void persistEvaluationChain(String interventionIri, String studentId,
                                        double effectiveness, double cost, double satisfaction) {
        // Find the assignment node for this intervention
        AssignmentNode matching = assignmentNeoRepo.findByIri(interventionIri).orElse(null);
        if (matching == null) return;

        ExecutionNode execNode = new ExecutionNode(
                "exec:" + UUID.randomUUID(), studentId, "system", matching);
        executionNeoRepo.save(execNode);

        EvaluationNode evalNode = new EvaluationNode(
                "eval:" + UUID.randomUUID(), effectiveness, cost, satisfaction, execNode);
        evaluationNeoRepo.save(evalNode);
    }

    @GetMapping("/populations/{conceptIri}")
    public ResponseEntity<Map<String, Object>> getPopulation(@PathVariable String conceptIri) {
        EvolutionEngine engine = interventionService.getEvolutionEngine();
        DecisionPopulation pop = engine.getPopulations().get(conceptIri);
        if (pop == null) return ResponseEntity.notFound().build();

        Map<String, Object> response = new HashMap<>();
        response.put("concept", conceptIri);
        response.put("generation", pop.getGenerationCounter());
        response.put("size", pop.size());
        response.put("activeCount", pop.getActiveMembers().size());

        List<Map<String, Object>> members = pop.getAllMembers().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("iri", a.getIri());
            m.put("decisionIri", a.getDecision().getIri());
            m.put("name", a.getDecision().getName());
            m.put("description", a.getDecision().getDescription());
            m.put("steps", a.getDecision().getSteps());
            m.put("scoreVector", a.getScoreVector());
            m.put("trials", a.getTrials());
            m.put("generation", a.getGeneration());
            m.put("status", a.getStatus().name());
            m.put("validationStatus", ontologyValidator.validate(a) ? "valid" : "invalid");
            if (a.getParents() != null && !a.getParents().isEmpty()) {
                m.put("parentDecisionIris", a.getParents().stream()
                        .map(p -> p.getDecision().getIri()).toList());
                m.put("parentNames", a.getParents().stream()
                        .map(p -> p.getDecision().getName()).toList());
            } else {
                m.put("parentDecisionIris", List.of());
                m.put("parentNames", List.of());
            }
            return m;
        }).toList();
        response.put("members", members);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/evolve")
    public ResponseEntity<String> triggerEvolution(@RequestParam("conceptIri") String conceptIri) {
        interventionService.evolveNiche(conceptIri);
        return ResponseEntity.ok("进化触发完成");
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        EvolutionEngine engine = interventionService.getEvolutionEngine();
        var globalMetrics = metrics.snapshot(engine.getPopulations());

        long eventCount = actionEventNeoRepo != null ? actionEventNeoRepo.count() : memoryEventCount.get();
        long interventionCount = assignmentNeoRepo != null ? assignmentNeoRepo.count() : memoryInterventionCount.get();

        Map<String, Object> response = new HashMap<>();
        response.put("totalFeedbacks", globalMetrics.getTotalFeedback());
        response.put("llmCalls", globalMetrics.getLlmCallCost());
        response.put("averageHypervolume", globalMetrics.getAverageHypervolume());
        response.put("nicheDiversity", globalMetrics.getNicheDiversityIndex());
        response.put("totalPopulations", engine.getPopulations().size());
        response.put("totalEvents", eventCount);
        response.put("totalStudents", studentRepo.count());
        response.put("totalInterventions", interventionCount);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/metrics/llm")
    public ResponseEntity<Map<String, Object>> getLlmMetrics() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalCalls", metrics.getLlmCalls());
        response.put("totalPromptTokens", metrics.getTotalPromptTokens());
        response.put("totalCompletionTokens", metrics.getTotalCompletionTokens());
        response.put("averageLatencyMs", metrics.getAverageLatencyMs());
        response.put("errorCount", metrics.getLlmErrorCount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/traces")
    public ResponseEntity<List<Map<String, Object>>> getTraces() {
        EvolutionEngine engine = interventionService.getEvolutionEngine();
        List<Map<String, Object>> traces = engine.getTraces().stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("type", t.getOperationType());
            m.put("timestamp", t.getTimestamp().toString());
            m.put("decision", t.getProducedDecision() != null ? t.getProducedDecision().getName() : "");
            m.put("decisionIri", t.getProducedDecision() != null ? t.getProducedDecision().getIri() : "");
            m.put("context", t.getContextDescription());
            // Include parent reference for genealogy tree
            if (t.getParentAssignments() != null && !t.getParentAssignments().isEmpty()) {
                m.put("parents", t.getParentAssignments().stream()
                        .map(a -> Map.of(
                                "name", a.getDecision() != null ? a.getDecision().getName() : "",
                                "iri", a.getDecision() != null ? a.getDecision().getIri() : ""
                        ))
                        .toList());
            } else {
                m.put("parents", List.of());
            }
            return m;
        }).toList();
        return ResponseEntity.ok(traces);
    }

    @GetMapping("/events")
    public ResponseEntity<List<?>> getEvents() {
        if (actionEventNeoRepo != null) {
            return ResponseEntity.ok(actionEventNeoRepo.findAll());
        }
        // Return in-memory events when Neo4j is not available
        synchronized (memoryEventStore) {
            return ResponseEntity.ok(new ArrayList<>(memoryEventStore));
        }
    }

    @GetMapping("/populations")
    public ResponseEntity<List<Map<String, Object>>> getAllPopulations() {
        EvolutionEngine engine = interventionService.getEvolutionEngine();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<String, DecisionPopulation> entry : engine.getPopulations().entrySet()) {
            DecisionPopulation pop = entry.getValue();
            Map<String, Object> view = new HashMap<>();
            view.put("conceptIri", entry.getKey());
            view.put("conceptLabel", pop.getConcept().getLabel());
            view.put("conceptCategory", pop.getConcept().getProperties().getOrDefault("category", ""));
            view.put("generation", pop.getGenerationCounter());
            view.put("size", pop.size());
            view.put("activeCount", pop.getActiveMembers().size());
            result.add(view);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ontology")
    public ResponseEntity<List<Map<String, Object>>> getOntology() {
        if (actionTypeNeoRepo != null) {
            List<ActionTypeNode> roots = actionTypeNeoRepo.findRoots();
            List<Map<String, Object>> result = roots.stream()
                    .map(this::buildOntologyTree)
                    .toList();
            return ResponseEntity.ok(result);
        }
        // Fallback: hardcoded ontology for in-memory mode
        return ResponseEntity.ok(buildHardcodedOntology());
    }

    private Map<String, Object> buildOntologyTree(ActionTypeNode node) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("iri", node.getIri());
        result.put("label", node.getLabel());
        result.put("comment", node.getComment());
        result.put("category", node.getCategory());
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            result.put("children", node.getChildren().stream()
                    .map(this::buildOntologyTree)
                    .toList());
        } else {
            result.put("children", List.of());
        }
        return result;
    }

    private List<Map<String, Object>> buildHardcodedOntology() {
        List<Map<String, Object>> roots = new ArrayList<>();

        Map<String, Object> behavioral = buildOntologyNode("Behavioral", "行为问题",
                "行为规范相关的问题类别", "Behavioral",
                buildOntologyNode("ClassroomDisruption", "课堂扰乱", "课堂纪律违反", "Behavioral"),
                buildOntologyNode("PeerConflict", "同学冲突", "学生之间的矛盾", "Behavioral"),
                buildOntologyNode("Noncompliance", "不服从管理", "不服从教师要求", "Behavioral"),
                buildOntologyNode("Cyberbullying", "网络欺凌", "网络言语攻击或霸凌", "Behavioral"),
                buildOntologyNode("Truancy", "逃课/旷课", "擅自缺课逃学", "Behavioral"),
                buildOntologyNode("SubstanceMisuse", "吸烟饮酒", "在校吸烟饮酒", "Behavioral")
        );
        Map<String, Object> academic = buildOntologyNode("Academic", "学业问题",
                "学业表现相关的问题", "Academic",
                buildOntologyNode("HomeworkMissing", "作业不交", "未按时完成作业", "Academic"),
                buildOntologyNode("Cheating", "考试作弊", "考试抄袭等学术不端", "Academic"),
                buildOntologyNode("LowPerformance", "成绩下滑", "成绩显著下降", "Academic"),
                buildOntologyNode("Inattention", "课堂走神", "注意力不集中", "Academic"),
                buildOntologyNode("LateSubmission", "迟交作业", "晚于截止日期提交", "Academic")
        );
        Map<String, Object> social = buildOntologyNode("Social", "社交问题",
                "社交互动相关问题", "Social",
                buildOntologyNode("SocialWithdrawal", "社交退缩", "回避集体活动", "Social"),
                buildOntologyNode("DisruptiveBehavior", "破坏公物", "故意损坏财物", "Social")
        );

        roots.add(behavioral);
        roots.add(academic);
        roots.add(social);
        return roots;
    }

    private Map<String, Object> buildOntologyNode(String iri, String label, String comment,
                                                   String category, Map<String, Object>... children) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("iri", "http://ontoevolve/education#" + iri);
        node.put("label", label);
        node.put("comment", comment);
        node.put("category", category);
        node.put("children", Arrays.asList(children));
        return node;
    }
}
