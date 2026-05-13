package com.ontoevolve.domain.education.controller;

import com.ontoevolve.core.kernel.DecisionPopulation;
import com.ontoevolve.core.kernel.EvolutionEngine;
import com.ontoevolve.core.kernel.EvolTrace;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/education")
public class EventController {

    private final InterventionService interventionService;
    private final MetricsCollector metrics;
    private final StudentRepository studentRepo;
    private final OntologyValidator ontologyValidator;

    private final ActionEventRepository actionEventNeoRepo;
    private final ActionTypeRepository actionTypeNeoRepo;
    private final EvaluationRepository evaluationNeoRepo;
    private final ExecutionRepository executionNeoRepo;
    private final AssignmentRepository assignmentNeoRepo;

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

    // ================================================================
    // Dashboard
    // ================================================================

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        EvolutionEngine engine = interventionService.getEvolutionEngine();
        Map<String, DecisionPopulation> populations = engine.getPopulations();
        var globalMetrics = metrics.snapshot(populations);

        Map<String, Object> response = new LinkedHashMap<>();

        // Stats
        Map<String, Object> stats = new LinkedHashMap<>();
        long todayEventCount = memoryEventStore.stream()
                .filter(e -> isToday((String) e.get("timestamp")))
                .count();
        long pendingCount = memoryEventStore.stream()
                .filter(e -> !Boolean.TRUE.equals(e.get("hasEvaluation"))).count();
        long activeInterventions = populations.values().stream()
                .mapToLong(p -> p.getActiveMembers().size()).sum();
        int totalGenerations = populations.values().stream()
                .mapToInt(DecisionPopulation::getGenerationCounter).max().orElse(0);

        stats.put("todayEvents", todayEventCount);
        stats.put("pendingEvaluations", pendingCount);
        stats.put("activeInterventions", activeInterventions);
        stats.put("totalGenerations", totalGenerations);
        stats.put("totalFeedbacks", globalMetrics.getTotalFeedback());
        stats.put("totalPopulations", populations.size());
        stats.put("totalStudents", studentRepo.count());
        stats.put("llmCalls", metrics.getLlmCalls());
        stats.put("evolutionRuns", metrics.getEvolutionRuns());
        response.put("stats", stats);

        // Recent activity (last 10 events)
        List<Map<String, Object>> recentActivity = new ArrayList<>();
        synchronized (memoryEventStore) {
            int from = Math.max(0, memoryEventStore.size() - 10);
            for (int i = memoryEventStore.size() - 1; i >= from; i--) {
                Map<String, Object> e = new LinkedHashMap<>(memoryEventStore.get(i));
                e.remove("candidates");
                recentActivity.add(e);
            }
        }
        response.put("recentActivity", recentActivity);

        // Top interventions by effectiveness
        List<Map<String, Object>> topInterventions = populations.values().stream()
                .flatMap(p -> p.getActiveMembers().stream())
                .filter(a -> a.getTrials() > 0)
                .sorted(Comparator.comparingDouble(a -> -averageScore(a)))
                .limit(8)
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("iri", a.getIri());
                    m.put("name", a.getDecision().getName());
                    m.put("description", a.getDecision().getDescription());
                    m.put("effectiveness", a.getScoreVector().length > 0 ? a.getScoreVector()[0] : 0);
                    m.put("cost", a.getScoreVector().length > 1 ? a.getScoreVector()[1] : 0);
                    m.put("satisfaction", a.getScoreVector().length > 2 ? a.getScoreVector()[2] : 0);
                    m.put("trials", a.getTrials());
                    m.put("conceptLabel", pConceptOf(a, populations));
                    return m;
                }).toList();
        response.put("topInterventions", topInterventions);

        // Niche health
        List<Map<String, Object>> nicheHealth = new ArrayList<>();
        for (var entry : populations.entrySet()) {
            DecisionPopulation pop = entry.getValue();
            List<Assignment> actives = pop.getActiveMembers();
            if (actives.isEmpty()) continue;
            Map<String, Object> nh = new LinkedHashMap<>();
            nh.put("conceptIri", entry.getKey());
            nh.put("conceptLabel", pop.getConcept().getLabel());
            nh.put("populationSize", actives.size());
            nh.put("generation", pop.getGenerationCounter());
            nh.put("avgEffectiveness", actives.stream().mapToDouble(a -> a.getScoreVector().length > 0 ? a.getScoreVector()[0] : 0).average().orElse(0));
            nh.put("avgCost", actives.stream().mapToDouble(a -> a.getScoreVector().length > 1 ? a.getScoreVector()[1] : 0).average().orElse(0));
            nh.put("avgSatisfaction", actives.stream().mapToDouble(a -> a.getScoreVector().length > 2 ? a.getScoreVector()[2] : 0).average().orElse(0));
            nh.put("totalTrials", actives.stream().mapToInt(Assignment::getTrials).sum());
            nh.put("eliteCount", actives.stream().filter(a -> a.getStatus() == Assignment.Status.ELITE).count());
            nicheHealth.add(nh);
        }
        response.put("nicheHealth", nicheHealth);

        return ResponseEntity.ok(response);
    }

    // ================================================================
    // Event submission
    // ================================================================

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

        String conceptIri = "";
        String conceptLabel = "";
        if (suggestion != null) {
            ActionType classifiedType = findConceptForIntervention(suggestion.getIri());
            if (classifiedType != null) {
                conceptIri = classifiedType.getIri();
                conceptLabel = classifiedType.getLabel();
            }
        }

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
        eventRecord.put("hasEvaluation", false);

        if (actionEventNeoRepo != null && suggestion != null) {
            persistEventWithRelation(event, suggestion);
        }

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

        memoryEventStore.add(eventRecord);
        memoryEventCount.incrementAndGet();
        if (suggestion != null) memoryInterventionCount.incrementAndGet();

        return ResponseEntity.ok(eventRecord);
    }

    // ================================================================
    // Evaluation
    // ================================================================

    @PostMapping("/evaluation")
    public ResponseEntity<Map<String, String>> submitEvaluation(@RequestBody Map<String, String> body) {
        String interventionIri = body.getOrDefault("interventionIri", "");
        String eventId = body.getOrDefault("eventId", "");
        String studentId = body.getOrDefault("studentId", "unknown");
        double effectiveness = Double.parseDouble(body.getOrDefault("effectiveness", "0.5"));
        double cost = Double.parseDouble(body.getOrDefault("cost", "0.3"));
        double satisfaction = Double.parseDouble(body.getOrDefault("satisfaction", "0.8"));

        interventionService.submitEvaluationByIntervention(
                interventionIri, studentId, effectiveness, cost, satisfaction
        );

        // Mark the event as evaluated
        if (!eventId.isEmpty()) {
            synchronized (memoryEventStore) {
                for (Map<String, Object> evt : memoryEventStore) {
                    if (eventId.equals(evt.get("eventId"))) {
                        evt.put("hasEvaluation", true);
                        evt.put("evalEffectiveness", effectiveness);
                        evt.put("evalCost", cost);
                        evt.put("evalSatisfaction", satisfaction);
                        break;
                    }
                }
            }
        }

        if (evaluationNeoRepo != null && executionNeoRepo != null && assignmentNeoRepo != null) {
            persistEvaluationChain(interventionIri, studentId, effectiveness, cost, satisfaction);
        }

        return ResponseEntity.ok(Map.of("message", "评价已提交"));
    }

    // ================================================================
    // Events list (with pagination & filtering)
    // ================================================================

    @GetMapping("/events")
    public ResponseEntity<Map<String, Object>> getEvents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "severity", required = false) String severity,
            @RequestParam(name = "studentId", required = false) String studentId) {

        List<Map<String, Object>> allEvents;
        if (actionEventNeoRepo != null) {
            allEvents = actionEventNeoRepo.findAll().stream()
                    .map(this::neoEventToMap)
                    .collect(Collectors.toList());
        } else {
            synchronized (memoryEventStore) {
                allEvents = new ArrayList<>(memoryEventStore);
            }
        }

        // Filter
        List<Map<String, Object>> filtered = allEvents.stream()
                .filter(e -> category == null || category.isEmpty()
                        || category.equalsIgnoreCase((String) e.getOrDefault("classifiedLabel", ""))
                        || (e.get("classifiedConcept") != null && e.get("classifiedConcept").toString().contains(category)))
                .filter(e -> severity == null || severity.isEmpty()
                        || severity.equalsIgnoreCase((String) e.getOrDefault("severity", "")))
                .filter(e -> studentId == null || studentId.isEmpty()
                        || (e.get("studentId") != null && e.get("studentId").toString().contains(studentId)))
                .collect(Collectors.toList());

        // Sort by timestamp descending
        filtered.sort((a, b) -> {
            String ta = (String) a.getOrDefault("timestamp", "");
            String tb = (String) b.getOrDefault("timestamp", "");
            return tb.compareTo(ta);
        });

        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<Map<String, Object>> pageItems = filtered.subList(fromIndex, toIndex);

        // Strip candidates from list view
        for (Map<String, Object> item : pageItems) {
            item.remove("candidates");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", pageItems);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", total);
        response.put("totalPages", (int) Math.ceil((double) total / size));
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> neoEventToMap(ActionEventNode node) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("eventId", node.getEventId());
        m.put("studentId", node.getStudentId());
        m.put("description", node.getDescription());
        m.put("location", node.getLocation());
        m.put("severity", node.getSeverity());
        m.put("classifiedLabel", node.getClassifiedConcept() != null ? node.getClassifiedConcept().getLabel() : "");
        m.put("classifiedConcept", node.getClassifiedConcept() != null ? node.getClassifiedConcept().getIri() : "");
        m.put("matchedIntervention", node.getMatchedIntervention());
        m.put("timestamp", node.getTimestamp() != null ? node.getTimestamp().toString() : "");
        m.put("hasEvaluation", false);
        return m;
    }

    // ================================================================
    // Event detail (full chain)
    // ================================================================

    @GetMapping("/events/{eventId}")
    public ResponseEntity<Map<String, Object>> getEventDetail(@PathVariable String eventId) {
        Map<String, Object> event = null;
        synchronized (memoryEventStore) {
            for (Map<String, Object> e : memoryEventStore) {
                if (eventId.equals(e.get("eventId"))) {
                    event = new LinkedHashMap<>(e);
                    break;
                }
            }
        }
        if (event == null) return ResponseEntity.notFound().build();

        // Enrich with intervention detail
        String interventionIri = (String) event.get("interventionIri");
        if (interventionIri != null && !interventionIri.isEmpty()) {
            Assignment found = findAssignmentByInterventionIri(interventionIri);
            if (found != null) {
                Map<String, Object> interventionDetail = new LinkedHashMap<>();
                interventionDetail.put("iri", found.getDecision().getIri());
                interventionDetail.put("name", found.getDecision().getName());
                interventionDetail.put("description", found.getDecision().getDescription());
                interventionDetail.put("steps", found.getDecision().getSteps());
                interventionDetail.put("scoreVector", found.getScoreVector());
                interventionDetail.put("trials", found.getTrials());
                interventionDetail.put("generation", found.getGeneration());
                interventionDetail.put("status", found.getStatus().name());
                if (found.getDecision() instanceof Intervention interv) {
                    interventionDetail.put("interventionType", interv.getInterventionType());
                    interventionDetail.put("requiresParentApproval", interv.isRequiresParentApproval());
                }
                event.put("interventionDetail", interventionDetail);
            }
        }

        return ResponseEntity.ok(event);
    }

    // ================================================================
    // Interventions library
    // ================================================================

    @GetMapping("/interventions")
    public ResponseEntity<List<Map<String, Object>>> getInterventions(
            @RequestParam(name = "conceptIri", required = false) String conceptIri,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "interventionType", required = false) String interventionType,
            @RequestParam(name = "sortBy", defaultValue = "effectiveness") String sortBy) {

        EvolutionEngine engine = interventionService.getEvolutionEngine();
        List<Map<String, Object>> result = new ArrayList<>();

        for (var entry : engine.getPopulations().entrySet()) {
            if (conceptIri != null && !conceptIri.isEmpty() && !conceptIri.equals(entry.getKey())) continue;
            DecisionPopulation pop = entry.getValue();

            for (Assignment a : pop.getAllMembers()) {
                if (status != null && !status.isEmpty() && !status.equalsIgnoreCase(a.getStatus().name())) continue;

                Intervention interv = (Intervention) a.getDecision();
                if (interventionType != null && !interventionType.isEmpty()
                        && !interventionType.equalsIgnoreCase(interv.getInterventionType())) continue;

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("iri", a.getIri());
                m.put("decisionIri", interv.getIri());
                m.put("name", interv.getName());
                m.put("description", interv.getDescription());
                m.put("steps", interv.getSteps());
                m.put("interventionType", interv.getInterventionType());
                m.put("requiresParentApproval", interv.isRequiresParentApproval());
                m.put("scoreVector", a.getScoreVector());
                m.put("trials", a.getTrials());
                m.put("generation", a.getGeneration());
                m.put("status", a.getStatus().name());
                m.put("conceptIri", entry.getKey());
                m.put("conceptLabel", pop.getConcept().getLabel());
                if (a.getParents() != null && !a.getParents().isEmpty()) {
                    m.put("parentNames", a.getParents().stream().map(p -> p.getDecision().getName()).toList());
                } else {
                    m.put("parentNames", List.of());
                }
                result.add(m);
            }
        }

        // Sort
        switch (sortBy) {
            case "trials" -> result.sort(Comparator.comparingInt(m -> -(int) m.get("trials")));
            case "generation" -> result.sort(Comparator.comparingInt(m -> -(int) m.get("generation")));
            case "cost" -> result.sort(Comparator.comparingDouble(m -> {
                double[] sv = (double[]) m.get("scoreVector");
                return sv.length > 1 ? sv[1] : 0;
            }));
            default -> result.sort(Comparator.comparingDouble(m -> {
                double[] sv = (double[]) m.get("scoreVector");
                return -(sv.length > 0 ? sv[0] : 0);
            }));
        }

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // Intervention lineage
    // ================================================================

    @GetMapping("/interventions/lineage")
    public ResponseEntity<Map<String, Object>> getInterventionLineage(@RequestParam("iri") String iri) {
        Assignment target = findAssignmentByIri(iri);
        if (target == null) return ResponseEntity.notFound().build();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("iri", target.getIri());
        result.put("name", target.getDecision().getName());
        result.put("generation", target.getGeneration());
        result.put("status", target.getStatus().name());

        // Ancestors (recursive)
        List<Map<String, Object>> ancestors = new ArrayList<>();
        collectAncestors(target, ancestors, new HashSet<>());
        result.put("ancestors", ancestors);

        // Descendants
        List<Map<String, Object>> descendants = new ArrayList<>();
        collectDescendants(target, descendants);
        result.put("descendants", descendants);

        // Evolution trace that produced this assignment
        List<EvolTrace> traces = interventionService.getEvolutionEngine().getTraces();
        List<Map<String, Object>> producingTraces = traces.stream()
                .filter(t -> t.getProducedAssignment() != null
                        && t.getProducedAssignment().getIri().equals(iri))
                .map(t -> {
                    Map<String, Object> tm = new LinkedHashMap<>();
                    tm.put("id", t.getId());
                    tm.put("type", t.getOperationType().name());
                    tm.put("timestamp", t.getTimestamp().toString());
                    tm.put("context", t.getContextDescription());
                    return tm;
                }).toList();
        result.put("producedBy", producingTraces);

        return ResponseEntity.ok(result);
    }

    private void collectAncestors(Assignment a, List<Map<String, Object>> result, Set<String> visited) {
        if (a.getParents() == null || a.getParents().isEmpty()) return;
        for (Assignment parent : a.getParents()) {
            if (!visited.add(parent.getIri())) continue;
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("iri", parent.getIri());
            pm.put("name", parent.getDecision().getName());
            pm.put("generation", parent.getGeneration());
            pm.put("status", parent.getStatus().name());
            result.add(pm);
            collectAncestors(parent, result, visited);
        }
    }

    private void collectDescendants(Assignment a, List<Map<String, Object>> result) {
        EvolutionEngine engine = interventionService.getEvolutionEngine();
        for (var pop : engine.getPopulations().values()) {
            for (Assignment member : pop.getAllMembers()) {
                if (member.getParents() != null) {
                    for (Assignment p : member.getParents()) {
                        if (p.getIri().equals(a.getIri())) {
                            Map<String, Object> dm = new LinkedHashMap<>();
                            dm.put("iri", member.getIri());
                            dm.put("name", member.getDecision().getName());
                            dm.put("generation", member.getGeneration());
                            dm.put("status", member.getStatus().name());
                            result.add(dm);
                        }
                    }
                }
            }
        }
    }

    // ================================================================
    // Graph data (concept instance relationships)
    // ================================================================

    @GetMapping("/graph")
    public ResponseEntity<Map<String, Object>> getGraphData(@RequestParam("conceptIri") String conceptIri) {
        EvolutionEngine engine = interventionService.getEvolutionEngine();
        DecisionPopulation pop = engine.getPopulations().get(conceptIri);
        if (pop == null) return ResponseEntity.notFound().build();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conceptIri", conceptIri);
        result.put("conceptLabel", pop.getConcept().getLabel());

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> nodeIds = new HashSet<>();

        // Concept node
        addNode(nodes, nodeIds, conceptIri, "ActionType", pop.getConcept().getLabel(),
                Map.of("category", pop.getConcept().getProperties().getOrDefault("category", "")));

        // Assignment nodes + edges
        for (Assignment a : pop.getAllMembers()) {
            String aId = a.getIri();
            addNode(nodes, nodeIds, aId, "Assignment", a.getDecision().getName(),
                    Map.of("status", a.getStatus().name(), "generation", String.valueOf(a.getGeneration()),
                            "effectiveness", String.format("%.2f", a.getScoreVector().length > 0 ? a.getScoreVector()[0] : 0),
                            "trials", String.valueOf(a.getTrials())));
            addEdge(edges, aId, conceptIri, "FOR_CONCEPT");

            // Intervention node
            String iId = a.getDecision().getIri();
            if (nodeIds.add(iId)) {
                Map<String, Object> iProps = new LinkedHashMap<>();
                iProps.put("description", a.getDecision().getDescription());
                if (a.getDecision() instanceof Intervention interv) {
                    iProps.put("interventionType", interv.getInterventionType());
                }
                nodes.add(Map.of("id", iId, "type", "Intervention", "label", a.getDecision().getName(), "properties", iProps));
            }
            addEdge(edges, aId, iId, "DECIDES");

            // Parent assignments
            if (a.getParents() != null) {
                for (Assignment parent : a.getParents()) {
                    addEdge(edges, aId, parent.getIri(), "HAS_PARENT");
                    if (nodeIds.add(parent.getIri())) {
                        nodes.add(Map.of("id", parent.getIri(), "type", "Assignment", "label", parent.getDecision().getName(),
                                "properties", Map.of("status", parent.getStatus().name(), "generation", String.valueOf(parent.getGeneration()))));
                    }
                }
            }
        }

        // Events classified to this concept (from memory store)
        String ns = "http://ontoevolve/education#";
        synchronized (memoryEventStore) {
            for (Map<String, Object> evt : memoryEventStore) {
                String cc = (String) evt.get("classifiedConcept");
                if (cc != null && (cc.equals(conceptIri) || cc.endsWith(conceptIri) || conceptIri.endsWith(cc))) {
                    String eId = (String) evt.get("eventId");
                    if (eId != null && nodeIds.add(eId)) {
                        nodes.add(Map.of("id", eId, "type", "ActionEvent", "label",
                                evt.getOrDefault("description", "").toString(),
                                "properties", Map.of("studentId", evt.getOrDefault("studentId", "").toString(),
                                        "severity", evt.getOrDefault("severity", "").toString())));
                    }
                    addEdge(edges, eId, conceptIri, "CLASSIFIED_AS");
                }
            }
        }

        result.put("nodes", nodes);
        result.put("edges", edges);
        return ResponseEntity.ok(result);
    }

    // ================================================================
    // Populations
    // ================================================================

    @GetMapping(value = "/populations", params = "conceptIri")
    public ResponseEntity<Map<String, Object>> getPopulation(@RequestParam("conceptIri") String conceptIri) {
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
            if (a.getDecision() instanceof Intervention interv) {
                m.put("interventionType", interv.getInterventionType());
            }
            return m;
        }).toList();
        response.put("members", members);

        // Pareto frontier data
        List<Assignment> actives = pop.getActiveMembers();
        if (!actives.isEmpty()) {
            List<Map<String, Double>> paretoPoints = actives.stream()
                    .filter(a -> a.getTrials() > 0 && a.getScoreVector().length >= 2)
                    .map(a -> Map.of(
                            "effectiveness", a.getScoreVector()[0],
                            "cost", a.getScoreVector()[1],
                            "satisfaction", a.getScoreVector().length > 2 ? a.getScoreVector()[2] : 0.0))
                    .toList();
            response.put("paretoPoints", paretoPoints);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/evolve")
    public ResponseEntity<String> triggerEvolution(@RequestParam("conceptIri") String conceptIri) {
        interventionService.evolveNiche(conceptIri);
        return ResponseEntity.ok("进化触发完成");
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
            view.put("eliteCount", pop.getActiveMembers().stream()
                    .filter(a -> a.getStatus() == Assignment.Status.ELITE).count());

            // Pareto summary
            List<Assignment> actives = pop.getActiveMembers();
            if (!actives.isEmpty()) {
                view.put("avgEffectiveness", actives.stream()
                        .mapToDouble(a -> a.getScoreVector().length > 0 ? a.getScoreVector()[0] : 0)
                        .average().orElse(0));
                view.put("avgCost", actives.stream()
                        .mapToDouble(a -> a.getScoreVector().length > 1 ? a.getScoreVector()[1] : 0)
                        .average().orElse(0));
            }
            result.add(view);
        }
        return ResponseEntity.ok(result);
    }

    // ================================================================
    // Ontology (enhanced with instance counts)
    // ================================================================

    @GetMapping("/ontology")
    public ResponseEntity<List<Map<String, Object>>> getOntology() {
        EvolutionEngine engine = interventionService.getEvolutionEngine();

        if (actionTypeNeoRepo != null) {
            List<ActionTypeNode> roots = actionTypeNeoRepo.findRoots();
            List<Map<String, Object>> result = roots.stream()
                    .map(node -> buildOntologyTree(node, engine))
                    .toList();
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.ok(buildHardcodedOntology(engine));
    }

    private Map<String, Object> buildOntologyTree(ActionTypeNode node, EvolutionEngine engine) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("iri", node.getIri());
        result.put("label", node.getLabel());
        result.put("comment", node.getComment());
        result.put("category", node.getCategory());

        DecisionPopulation pop = engine.getPopulations().get(node.getIri());
        result.put("assignmentCount", pop != null ? pop.getActiveMembers().size() : 0);
        result.put("eventCount", countEventsForConcept(node.getIri()));

        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            result.put("children", node.getChildren().stream()
                    .map(child -> buildOntologyTree(child, engine))
                    .toList());
        } else {
            result.put("children", List.of());
        }
        return result;
    }

    private long countEventsForConcept(String conceptIri) {
        synchronized (memoryEventStore) {
            return memoryEventStore.stream()
                    .filter(e -> conceptIri.equals(e.get("classifiedConcept")))
                    .count();
        }
    }

    private List<Map<String, Object>> buildHardcodedOntology(EvolutionEngine engine) {
        String ns = "http://ontoevolve/education#";
        List<Map<String, Object>> roots = new ArrayList<>();

        Map<String, Object> behavioral = buildOntologyNodeWithCounts(ns + "Behavioral", "行为问题",
                "行为规范相关的问题类别", "Behavioral", engine,
                buildOntologyNodeWithCounts(ns + "ClassroomDisruption", "课堂扰乱", "课堂纪律违反", "Behavioral", engine),
                buildOntologyNodeWithCounts(ns + "PeerConflict", "同学冲突", "学生之间的矛盾", "Behavioral", engine),
                buildOntologyNodeWithCounts(ns + "Noncompliance", "不服从管理", "不服从教师要求", "Behavioral", engine),
                buildOntologyNodeWithCounts(ns + "Cyberbullying", "网络欺凌", "网络言语攻击或霸凌", "Behavioral", engine),
                buildOntologyNodeWithCounts(ns + "Truancy", "逃课/旷课", "擅自缺课逃学", "Behavioral", engine),
                buildOntologyNodeWithCounts(ns + "SubstanceMisuse", "吸烟饮酒", "在校吸烟饮酒", "Behavioral", engine)
        );
        Map<String, Object> academic = buildOntologyNodeWithCounts(ns + "Academic", "学业问题",
                "学业表现相关的问题", "Academic", engine,
                buildOntologyNodeWithCounts(ns + "HomeworkMissing", "作业不交", "未按时完成作业", "Academic", engine),
                buildOntologyNodeWithCounts(ns + "Cheating", "考试作弊", "考试抄袭等学术不端", "Academic", engine),
                buildOntologyNodeWithCounts(ns + "LowPerformance", "成绩下滑", "成绩显著下降", "Academic", engine),
                buildOntologyNodeWithCounts(ns + "Inattention", "课堂走神", "注意力不集中", "Academic", engine),
                buildOntologyNodeWithCounts(ns + "LateSubmission", "迟交作业", "晚于截止日期提交", "Academic", engine)
        );
        Map<String, Object> social = buildOntologyNodeWithCounts(ns + "Social", "社交问题",
                "社交互动相关问题", "Social", engine,
                buildOntologyNodeWithCounts(ns + "SocialWithdrawal", "社交退缩", "回避集体活动", "Social", engine),
                buildOntologyNodeWithCounts(ns + "DisruptiveBehavior", "破坏公物", "故意损坏财物", "Social", engine)
        );

        roots.add(behavioral);
        roots.add(academic);
        roots.add(social);
        return roots;
    }

    private Map<String, Object> buildOntologyNodeWithCounts(String iri, String label, String comment,
                                                             String category, EvolutionEngine engine,
                                                             Map<String, Object>... children) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("iri", iri);
        node.put("label", label);
        node.put("comment", comment);
        node.put("category", category);
        DecisionPopulation pop = engine.getPopulations().get(iri);
        node.put("assignmentCount", pop != null ? pop.getActiveMembers().size() : 0);
        node.put("eventCount", countEventsForConcept(iri));
        node.put("children", Arrays.asList(children));
        return node;
    }

    // ================================================================
    // Metrics
    // ================================================================

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

    // ================================================================
    // Traces
    // ================================================================

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

    // ================================================================
    // Private helpers
    // ================================================================

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

    private Assignment findAssignmentByInterventionIri(String interventionIri) {
        return interventionService.getEvolutionEngine().getPopulations().values().stream()
                .flatMap(pop -> pop.getAllMembers().stream())
                .filter(a -> a.getDecision().getIri().equals(interventionIri))
                .findFirst().orElse(null);
    }

    private Assignment findAssignmentByIri(String iri) {
        return interventionService.getEvolutionEngine().getPopulations().values().stream()
                .flatMap(pop -> pop.getAllMembers().stream())
                .filter(a -> a.getIri().equals(iri))
                .findFirst().orElse(null);
    }

    private double averageScore(Assignment a) {
        double[] sv = a.getScoreVector();
        if (sv.length == 0) return 0;
        double sum = 0;
        for (double v : sv) sum += v;
        return sum / sv.length;
    }

    private String pConceptOf(Assignment a, Map<String, DecisionPopulation> pops) {
        for (var entry : pops.entrySet()) {
            if (entry.getValue().getAllMembers().contains(a)) {
                return entry.getValue().getConcept().getLabel();
            }
        }
        return "";
    }

    private boolean isToday(String timestamp) {
        if (timestamp == null) return false;
        try {
            Instant instant = Instant.parse(timestamp);
            LocalDate eventDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            return eventDate.equals(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private void addNode(List<Map<String, Object>> nodes, Set<String> ids, String id, String type, String label, Map<String, Object> props) {
        if (!ids.add(id)) return;
        nodes.add(Map.of("id", id, "type", type, "label", label, "properties", props));
    }

    private void addEdge(List<Map<String, Object>> edges, String source, String target, String type) {
        edges.add(Map.of("source", source, "target", target, "type", type));
    }

    private void persistEventWithRelation(ActionEvent event, Intervention suggestion) {
        ActionTypeNode conceptNode = actionTypeNeoRepo.findById(suggestion.getIri())
                .orElseGet(() -> {
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

    private void persistEvaluationChain(String interventionIri, String studentId,
                                        double effectiveness, double cost, double satisfaction) {
        AssignmentNode matching = assignmentNeoRepo.findByIri(interventionIri).orElse(null);
        if (matching == null) return;

        ExecutionNode execNode = new ExecutionNode(
                "exec:" + UUID.randomUUID(), studentId, "system", matching);
        executionNeoRepo.save(execNode);

        EvaluationNode evalNode = new EvaluationNode(
                "eval:" + UUID.randomUUID(), effectiveness, cost, satisfaction, execNode);
        evaluationNeoRepo.save(evalNode);
    }
}
