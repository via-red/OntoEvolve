package com.ontoevolve.graphstore.store;

import com.ontoevolve.core.kernel.DecisionPopulation;
import com.ontoevolve.core.kernel.EvolTrace;
import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.spi.PopulationStore;
import com.ontoevolve.graphstore.mapper.ModelMapper;
import com.ontoevolve.graphstore.node.*;
import com.ontoevolve.graphstore.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Hybrid PopulationStore: in-memory hot cache + Neo4j persistence backend.
 *
 * <p>Evolution hot-path reads/writes go through the local cache.
 * Neo4j is kept in sync via write-through operations and is used
 * to reload state on restart.
 */
public class Neo4jPopulationStore implements PopulationStore {

    private static final Logger log = LoggerFactory.getLogger(Neo4jPopulationStore.class);

    private final Map<String, DecisionPopulation> cache = new HashMap<>();
    private final Map<String, Integer> feedbackCounters = new ConcurrentHashMap<>();
    private final List<EvolTrace> traces = Collections.synchronizedList(new ArrayList<>());

    private final AssignmentRepository assignmentRepo;
    private final ActionTypeRepository actionTypeRepo;
    private final InterventionRepository interventionRepo;
    private final EvolTraceRepository traceRepo;

    public Neo4jPopulationStore(AssignmentRepository assignmentRepo,
                                ActionTypeRepository actionTypeRepo,
                                InterventionRepository interventionRepo,
                                EvolTraceRepository traceRepo) {
        this.assignmentRepo = assignmentRepo;
        this.actionTypeRepo = actionTypeRepo;
        this.interventionRepo = interventionRepo;
        this.traceRepo = traceRepo;
    }

    @Override
    public DecisionPopulation findOrCreatePopulation(Concept concept, int maxSize) {
        return cache.computeIfAbsent(concept.getIri(), k -> {
            // Try to load from Neo4j first
            DecisionPopulation loaded = loadPopulation(concept, maxSize);
            if (loaded != null) {
                log.debug("Loaded population for {} from Neo4j (size={})", concept.getIri(), loaded.size());
                return loaded;
            }
            log.debug("Creating new population for {}", concept.getIri());
            return new DecisionPopulation(concept, maxSize);
        });
    }

    @Override
    public Optional<DecisionPopulation> getPopulation(String conceptIri) {
        return Optional.ofNullable(cache.get(conceptIri));
    }

    @Override
    public Map<String, DecisionPopulation> getAllPopulations() {
        return Collections.unmodifiableMap(cache);
    }

    @Override
    public void addMember(Concept concept, Assignment assignment) {
        DecisionPopulation pop = cache.get(concept.getIri());
        if (pop != null) {
            pop.addMember(assignment);
        }
        // Persist to Neo4j
        saveAssignmentToNeo4j(assignment);
    }

    @Override
    public void replaceMembers(Concept concept, List<Assignment> members) {
        DecisionPopulation pop = cache.get(concept.getIri());
        if (pop != null) {
            pop.replaceMembers(members);
        }
        // Sync entire population to Neo4j: save all current members
        for (Assignment member : members) {
            saveAssignmentToNeo4j(member);
        }
    }

    @Override
    public int incrementGeneration(Concept concept) {
        DecisionPopulation pop = cache.get(concept.getIri());
        if (pop != null) {
            pop.incrementGeneration();
            return pop.getGenerationCounter();
        }
        return 0;
    }

    @Override
    public int getGeneration(Concept concept) {
        DecisionPopulation pop = cache.get(concept.getIri());
        return pop != null ? pop.getGenerationCounter() : 0;
    }

    @Override
    public int incrementFeedbackCounter(Concept concept) {
        return feedbackCounters.merge(concept.getIri(), 1, Integer::sum);
    }

    @Override
    public int getFeedbackCount(Concept concept) {
        return feedbackCounters.getOrDefault(concept.getIri(), 0);
    }

    @Override
    public void resetFeedbackCounter(Concept concept) {
        feedbackCounters.put(concept.getIri(), 0);
    }

    @Override
    public void addTrace(EvolTrace trace) {
        traces.add(trace);
        // Also persist to Neo4j
        try {
            persistTrace(trace);
        } catch (Exception e) {
            log.warn("Failed to persist trace {} to Neo4j: {}", trace.getId(), e.getMessage());
        }
    }

    @Override
    public List<EvolTrace> getTraces() {
        return Collections.unmodifiableList(traces);
    }

    @Override
    public void clear() {
        cache.clear();
        feedbackCounters.clear();
        traces.clear();
    }

    // ──────────────────────────────────────────────
    // Neo4j persistence helpers
    // ──────────────────────────────────────────────

    private void saveAssignmentToNeo4j(Assignment assignment) {
        try {
            AssignmentNode node = ModelMapper.assignmentToNode(assignment);
            // Ensure related nodes exist (save parent chain first)
            saveConceptChain(node.getConcept());
            if (node.getIntervention() != null) {
                interventionRepo.save(node.getIntervention());
            }
            assignmentRepo.save(node);
        } catch (Exception e) {
            log.warn("Failed to persist assignment {} to Neo4j: {}", assignment.getIri(), e.getMessage());
        }
    }

    private void saveConceptChain(ActionTypeNode node) {
        if (node == null) return;
        if (node.getParent() != null) {
            saveConceptChain(node.getParent());
        }
        actionTypeRepo.save(node);
    }

    private void persistTrace(EvolTrace trace) {
        AssignmentNode produced = trace.getProducedAssignment() != null
                ? ModelMapper.assignmentToNode(trace.getProducedAssignment()) : null;
        InterventionNode decision = trace.getProducedDecision() != null
                ? ModelMapper.decisionToNode(trace.getProducedDecision()) : null;
        List<AssignmentNode> parents = trace.getParentAssignments() != null
                ? trace.getParentAssignments().stream()
                    .map(ModelMapper::assignmentToNode)
                    .collect(Collectors.toList())
                : List.of();

        EvolTraceNode node = ModelMapper.traceToNode(trace, produced, decision, parents);
        if (produced != null) {
            assignmentRepo.save(produced);
        }
        if (decision != null) {
            interventionRepo.save(decision);
        }
        for (AssignmentNode parent : parents) {
            assignmentRepo.save(parent);
        }
        traceRepo.save(node);
    }

    // ──────────────────────────────────────────────
    // Population reconstruction from Neo4j
    // ──────────────────────────────────────────────

    private DecisionPopulation loadPopulation(Concept concept, int maxSize) {
        List<AssignmentNode> nodes = assignmentRepo.findByConceptIri(concept.getIri());
        if (nodes.isEmpty()) return null;

        DecisionPopulation pop = new DecisionPopulation(concept, maxSize);
        for (AssignmentNode node : nodes) {
            Assignment assignment = reconstructAssignment(node);
            if (assignment != null) {
                pop.addMember(assignment);
            }
        }

        // Restore generation from the highest stored generation
        int maxGen = nodes.stream()
                .mapToInt(AssignmentNode::getGeneration)
                .max().orElse(0);
        for (int i = 0; i < maxGen; i++) {
            pop.incrementGeneration();
        }

        log.info("Reconstructed population for {} with {} members, gen={}",
                concept.getIri(), pop.size(), pop.getGenerationCounter());
        return pop;
    }

    private Assignment reconstructAssignment(AssignmentNode node) {
        try {
            Concept concept = ModelMapper.nodeToConcept(node.getConcept());
            Decision decision = ModelMapper.nodeToDecision(node.getIntervention());

            Assignment assignment = new Assignment(
                    node.getIri(), decision, concept, ModelMapper.SCORE_DIMENSION);

            // Restore mutable fields via setters where available
            assignment.setGeneration(node.getGeneration());
            assignment.setStatus(Assignment.Status.valueOf(node.getStatus()));
            if (node.getLastUpdated() != null) {
                setField(assignment, "lastUpdated", node.getLastUpdated());
            }

            // Restore score vector and trials via reflection
            double[] scores = new double[ModelMapper.SCORE_DIMENSION];
            scores[ModelMapper.IDX_EFFECTIVENESS] = node.getEffectiveness();
            scores[ModelMapper.IDX_COST] = node.getCost();
            scores[ModelMapper.IDX_SATISFACTION] = node.getSatisfaction();
            setField(assignment, "scoreVector", scores);

            double[] variances = new double[ModelMapper.SCORE_DIMENSION];
            variances[ModelMapper.IDX_EFFECTIVENESS] = node.getVarianceEffectiveness();
            variances[ModelMapper.IDX_COST] = node.getVarianceCost();
            variances[ModelMapper.IDX_SATISFACTION] = node.getVarianceSatisfaction();
            setField(assignment, "varianceVector", variances);

            setField(assignment, "trials", node.getTrials());

            return assignment;
        } catch (Exception e) {
            log.error("Failed to reconstruct assignment {}: {}", node.getIri(), e.getMessage());
            return null;
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        if (field != null) {
            field.setAccessible(true);
            field.set(target, value);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), fieldName);
            }
            return null;
        }
    }
}
