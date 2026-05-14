package com.ontoevolve.saas.service;

import com.ontoevolve.core.kernel.DecisionPopulation;
import com.ontoevolve.core.kernel.EvolutionEngine;
import com.ontoevolve.core.model.*;
import com.ontoevolve.core.spi.Matcher;
import com.ontoevolve.infra.metrics.MetricsCollector;
import com.ontoevolve.saas.config.*;
import com.ontoevolve.saas.dto.*;
import com.ontoevolve.saas.store.DomainStore;
import com.ontoevolve.saas.store.DomainStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generic domain orchestrator — the central pipeline for event processing,
 * feedback submission, population management, and evolution triggering.
 */
@Service
public class DomainOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(DomainOrchestrationService.class);
    private static final int MAX_POPULATION_SIZE = 50;

    private final DomainCache domainCache;
    private final DynamicOntologyService ontologyService;
    private final ConfigDrivenClassifier classifier;
    private final DomainStoreManager storeManager;
    private final Matcher<Concept, Decision, Assignment> matcher;
    private final EvolutionEngine evolutionEngine;
    private final MetricsCollector metrics;

    @SuppressWarnings("unchecked")
    public DomainOrchestrationService(DomainCache domainCache,
                                       DynamicOntologyService ontologyService,
                                       ConfigDrivenClassifier classifier,
                                       DomainStoreManager storeManager,
                                       Matcher<?, ?, ?> matcher,
                                       EvolutionEngine evolutionEngine,
                                       MetricsCollector metrics) {
        this.domainCache = domainCache;
        this.ontologyService = ontologyService;
        this.classifier = classifier;
        this.storeManager = storeManager;
        this.matcher = (Matcher<Concept, Decision, Assignment>) matcher;
        this.evolutionEngine = evolutionEngine;
        this.metrics = metrics;
    }

    /**
     * Process an event: validate → classify → seed → match → store → return result.
     */
    public EventResult processEvent(String domainId, Map<String, Object> attributes) {
        DomainDefinition def = domainCache.get(domainId);
        if (def == null) throw new IllegalArgumentException("Domain not found: " + domainId);

        // 1. Validate fields against domain definition
        validateFields(def, attributes);

        // 2. Create InputEvent
        String recordId = UUID.randomUUID().toString();
        String description = (String) attributes.getOrDefault("description", "");
        String subjectId = (String) attributes.getOrDefault("subjectId", null);
        Map<String, Object> eventAttrs = new HashMap<>(attributes);
        eventAttrs.put("domainId", domainId);
        InputEvent event = new InputEvent(recordId, Instant.now(), description,
                "saas", subjectId, eventAttrs);

        // 3. Classify
        Concept concept = classifier.classify(domainId, event);

        // 4. Seed population if empty
        DomainStore store = storeManager.getStore(domainId);
        seedPopulationIfEmpty(def, store, concept);

        // 5. Match (with hierarchy walk if no match at current level)
        Matcher.Context ctx = new Matcher.Context(
                subjectId != null ? subjectId : "anonymous", attributes);
        Assignment assignment = findMatchHierarchy(def, store, concept, ctx);

        // 6. Store record with assignment IRI for feedback correlation
        eventAttrs.put("assignmentIri", assignment.getIri());
        eventAttrs.put("conceptIri", concept.getIri());
        store.saveRecord(event);

        // 7. Build and return result
        EventResult result = new EventResult();
        result.setRecordId(recordId);
        result.setConceptIri(concept.getIri());
        result.setConceptLabel(concept.getLabel());
        result.setDecisionIri(assignment.getDecision().getIri());
        result.setDecisionName(assignment.getDecision().getName());
        result.setSteps(assignment.getDecision().getSteps());
        result.setAssignmentIri(assignment.getIri());
        result.setScoreVector(assignment.getScoreVector());
        result.setAttributes(attributes);
        return result;
    }

    /**
     * Submit feedback for a record and assignment.
     */
    public void submitFeedback(String domainId, String recordId, String assignmentIri,
                                Map<String, Double> dimensionScores,
                                Map<String, Object> attributes) {
        DomainDefinition def = domainCache.get(domainId);
        if (def == null) throw new IllegalArgumentException("Domain not found: " + domainId);

        DomainStore store = storeManager.getStore(domainId);

        // Find assignment across all populations in this domain
        Assignment assignment = findAssignmentInDomain(store, assignmentIri);
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment not found: " + assignmentIri);
        }

        // Convert dimension name→value map to ordered double[]
        List<FeedbackDimensionConfig> dims = def.getFeedbackDimensions();
        if (dims.isEmpty()) {
            throw new IllegalStateException("No feedback dimensions configured for domain: " + domainId);
        }
        double[] scores = mapScores(dims, dimensionScores);

        // Create Execution
        Execution execution = new Execution(
                "exec:" + UUID.randomUUID(),
                assignment,
                assignment.getConcept().getIri(),
                "user",
                attributes != null ? Map.copyOf(attributes) : Map.of());

        // Create Feedback
        Feedback feedback = new Feedback(
                "fb:" + UUID.randomUUID(),
                execution,
                scores,
                attributes != null ? Map.copyOf(attributes) : Map.of());

        // Update assignment's online statistics
        assignment.updateScore(scores);

        // Store feedback
        store.saveFeedback(feedback);

        // Notify evolution engine for potential light evolution
        evolutionEngine.recordFeedback(assignment.getConcept());

        log.info("Feedback recorded for domain={} record={} assignment={} scores={}",
                domainId, recordId, assignmentIri, dimensionScores);
    }

    /**
     * Get all populations for a domain.
     */
    public List<PopulationDTO> getPopulations(String domainId) {
        DomainStore store = storeManager.getStore(domainId);
        Map<String, DecisionPopulation> all = store.getAllPopulations();
        return all.values().stream().map(pop -> {
            PopulationDTO dto = new PopulationDTO();
            dto.setConceptIri(pop.getConcept().getIri());
            dto.setConceptLabel(pop.getConcept().getLabel());
            dto.setSize(pop.size());
            dto.setGeneration(pop.getGenerationCounter());
            dto.setAssignments(pop.getAllMembers().stream().map(a -> {
                PopulationDTO.AssignmentDTO ad = new PopulationDTO.AssignmentDTO();
                ad.setIri(a.getIri());
                ad.setDecisionIri(a.getDecision().getIri());
                ad.setDecisionName(a.getDecision().getName());
                ad.setSteps(a.getDecision().getSteps());
                ad.setScoreVector(a.getScoreVector());
                ad.setStatus(a.getStatus() == null ? "UNKNOWN" : a.getStatus().name());
                ad.setTrials(a.getTrials());
                ad.setGeneration(a.getGeneration());
                return ad;
            }).collect(Collectors.toList()));
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Trigger full evolution on a specific concept niche.
     */
    public void triggerEvolution(String domainId, String conceptIri) {
        Optional<Concept> conceptOpt = ontologyService.findConcept(domainId, conceptIri);
        if (conceptOpt.isEmpty()) {
            throw new IllegalArgumentException("Concept not found: " + conceptIri);
        }
        evolutionEngine.runFullEvolution(conceptOpt.get());
        log.info("Triggered evolution for domain={} concept={}", domainId, conceptIri);
    }

    /**
     * Get graph data for visualization (concept hierarchy + assignment nodes).
     */
    public GraphDataDTO getGraphData(String domainId, String conceptIri) {
        DomainStore store = storeManager.getStore(domainId);
        List<GraphDataDTO.GraphNodeDTO> nodes = new ArrayList<>();
        List<GraphDataDTO.GraphEdgeDTO> edges = new ArrayList<>();

        List<Concept> concepts;
        if (conceptIri != null && !conceptIri.isBlank()) {
            concepts = ontologyService.getConceptWithAncestors(domainId, conceptIri);
            ontologyService.findChildren(domainId, concepts.get(0))
                    .forEach(c -> addConceptSubtree(c, concepts));
        } else {
            concepts = ontologyService.getConcepts(domainId);
        }

        // Add concept nodes
        for (Concept c : concepts) {
            GraphDataDTO.GraphNodeDTO node = new GraphDataDTO.GraphNodeDTO();
            node.setId(c.getIri());
            node.setLabel(c.getLabel());
            node.setConceptIri(c.getIri());
            node.setGroup("concept");
            node.setProperties(c.getProperties());
            node.setSize(10);

            // Count active assignments for this concept
            DecisionPopulation pop = store.getPopulation(c.getIri()).orElse(null);
            if (pop != null) {
                node.setSize(10 + pop.getActiveMembers().size());
            }
            nodes.add(node);

            // Add hierarchy edge
            if (c.hasParent()) {
                GraphDataDTO.GraphEdgeDTO edge = new GraphDataDTO.GraphEdgeDTO();
                edge.setSource(c.getParentConcept().getIri());
                edge.setTarget(c.getIri());
                edge.setLabel("subClassOf");
                edge.setType("hierarchy");
                edges.add(edge);
            }
        }

        // Add assignment nodes and their edges to concepts
        for (Map.Entry<String, DecisionPopulation> entry : store.getAllPopulations().entrySet()) {
            for (Assignment a : entry.getValue().getAllMembers()) {
                GraphDataDTO.GraphNodeDTO node = new GraphDataDTO.GraphNodeDTO();
                node.setId(a.getIri());
                node.setLabel(a.getDecision().getName());
                node.setConceptIri(entry.getKey());
                node.setGroup("assignment");
                node.setScoreVector(a.getScoreVector());
                node.setSize(5);
                nodes.add(node);

                GraphDataDTO.GraphEdgeDTO edge = new GraphDataDTO.GraphEdgeDTO();
                edge.setSource(entry.getKey());
                edge.setTarget(a.getIri());
                edge.setLabel("assigned");
                edge.setType("assignment");
                edges.add(edge);
            }
        }

        GraphDataDTO dto = new GraphDataDTO();
        dto.setNodes(nodes);
        dto.setEdges(edges);
        return dto;
    }

    /**
     * Get system metrics for a domain.
     */
    public Map<String, Object> getMetrics(String domainId) {
        DomainStore store = storeManager.getStore(domainId);
        Map<String, DecisionPopulation> all = store.getAllPopulations();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("domainId", domainId);
        m.put("totalPopulations", all.size());
        m.put("totalAssignments", all.values().stream()
                .mapToInt(DecisionPopulation::size).sum());
        m.put("totalRecords", store.countRecords(Map.of()));
        m.put("totalFeedbacks", all.values().stream()
                .flatMap(p -> p.getAllMembers().stream())
                .mapToInt(Assignment::getTrials).sum());

        // Average assignment scores per population
        Map<String, double[]> popAverages = new LinkedHashMap<>();
        for (Map.Entry<String, DecisionPopulation> entry : all.entrySet()) {
            List<Assignment> members = entry.getValue().getAllMembers();
            if (!members.isEmpty()) {
                int dim = members.get(0).getScoreVector().length;
                double[] avg = new double[dim];
                for (Assignment a : members) {
                    double[] sv = a.getScoreVector();
                    for (int i = 0; i < dim; i++) avg[i] += sv[i];
                }
                for (int i = 0; i < dim; i++) avg[i] /= members.size();
                String label = entry.getValue().getConcept().getLabel();
                popAverages.put(label, avg);
            }
        }
        m.put("populationAverages", popAverages);

        return m;
    }

    // ---- Private helpers ----

    private void validateFields(DomainDefinition def, Map<String, Object> attributes) {
        List<FieldDefinitionConfig> fields = def.getFieldDefinitions();
        if (fields == null || fields.isEmpty()) return;

        for (FieldDefinitionConfig field : fields) {
            if (field.isRequired()) {
                Object val = attributes.get(field.getFieldName());
                if (val == null || val.toString().isBlank()) {
                    throw new IllegalArgumentException(
                            "Required field '" + field.getFieldName() + "' is missing");
                }
            }
            // ENUM validation
            if (field.getFieldType() == FieldDefinitionConfig.FieldType.ENUM
                    && field.getEnumValues() != null && !field.getEnumValues().isEmpty()) {
                Object val = attributes.get(field.getFieldName());
                if (val != null && !field.getEnumValues().contains(val.toString())) {
                    throw new IllegalArgumentException(
                            "Invalid value '" + val + "' for field '" + field.getFieldName()
                                    + "'. Allowed: " + field.getEnumValues());
                }
            }
        }
    }

    private void seedPopulationIfEmpty(DomainDefinition def, DomainStore store, Concept concept) {
        DecisionPopulation pop = store.findOrCreatePopulation(concept, MAX_POPULATION_SIZE);
        if (!pop.getActiveMembers().isEmpty()) return;

        List<DecisionTypeConfig> decisionTypes = def.getDecisionTypes();
        List<ConceptMappingConfig> mappings = def.getConceptMappings();
        List<FeedbackDimensionConfig> dims = def.getFeedbackDimensions();
        int dimensionCount = Math.max(dims.size(), 1); // at least 1

        // Find which decision types map to this concept
        List<String> mappedIris = mappings.stream()
                .filter(m -> m.getConceptIri().equals(concept.getIri()))
                .findFirst()
                .map(m -> {
                    if (m.isAutoDerive()) {
                        return decisionTypes.stream()
                                .map(DecisionTypeConfig::getIri)
                                .collect(Collectors.toList());
                    }
                    return m.getDecisionIris();
                })
                .orElseGet(() -> {
                    // No explicit mapping — check for wildcard/autoDerive on parent
                    boolean anyAutoDerive = mappings.stream()
                            .anyMatch(ConceptMappingConfig::isAutoDerive);
                    if (anyAutoDerive) {
                        return decisionTypes.stream()
                                .map(DecisionTypeConfig::getIri)
                                .collect(Collectors.toList());
                    }
                    // Try parent concept mappings recursively
                    if (concept.hasParent()) {
                        return List.of(); // parent will handle it
                    }
                    return decisionTypes.stream()
                            .map(DecisionTypeConfig::getIri)
                            .collect(Collectors.toList());
                });

        for (String decisionIri : mappedIris) {
            DecisionTypeConfig dtc = decisionTypes.stream()
                    .filter(d -> d.getIri().equals(decisionIri))
                    .findFirst().orElse(null);
            if (dtc == null) continue;

            String seedIri = "seed:" + concept.getIri() + "/" + dtc.getIri();
            Decision decision = new Decision(seedIri, dtc.getName(),
                    dtc.getDescription() != null ? dtc.getDescription() : dtc.getName(),
                    dtc.getStepsTemplate());
            Assignment assignment = new Assignment(
                    "ass:" + seedIri + ":" + UUID.randomUUID(),
                    decision, concept, dimensionCount);
            assignment.setStatus(Assignment.Status.ACTIVE);
            store.addMember(concept, assignment);
        }

        log.info("Seeded population for concept '{}' with {} assignments",
                concept.getLabel(), mappedIris.size());
    }

    private Assignment findMatchHierarchy(DomainDefinition def, DomainStore store,
                                           Concept concept, Matcher.Context ctx) {
        DecisionPopulation pop = store.findOrCreatePopulation(concept, MAX_POPULATION_SIZE);
        List<Assignment> active = pop.getActiveMembers();
        if (!active.isEmpty()) {
            return matcher.match(concept, ctx, active);
        }
        // Walk up parent hierarchy
        if (concept.hasParent()) {
            return findMatchHierarchy(def, store, concept.getParentConcept(), ctx);
        }
        throw new IllegalStateException("No match found for concept hierarchy: " + concept.getLabel());
    }

    private Assignment findAssignmentInDomain(DomainStore store, String assignmentIri) {
        for (DecisionPopulation pop : store.getAllPopulations().values()) {
            for (Assignment a : pop.getAllMembers()) {
                if (a.getIri().equals(assignmentIri)) return a;
            }
        }
        return null;
    }

    private double[] mapScores(List<FeedbackDimensionConfig> dims,
                                Map<String, Double> dimensionScores) {
        double[] scores = new double[dims.size()];
        for (int i = 0; i < dims.size(); i++) {
            FeedbackDimensionConfig dim = dims.get(i);
            Double val = dimensionScores.get(dim.getName());
            if (val != null) {
                double clamped = Math.max(dim.getMin(), Math.min(dim.getMax(), val));
                scores[i] = dim.isHigherIsBetter() ? clamped : (1.0 - clamped);
            } else {
                scores[i] = 0.0;
            }
        }
        return scores;
    }

    private void addConceptSubtree(Concept concept, List<Concept> accumulator) {
        accumulator.add(concept);
    }
}
