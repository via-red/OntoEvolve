package com.ontoevolve.core.store;

import com.ontoevolve.core.kernel.DecisionPopulation;
import com.ontoevolve.core.kernel.EvolTrace;
import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.spi.PopulationStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory implementation of {@link PopulationStore}.
 *
 * <p>Extracted from EvolutionEngine's original inline state. Used when
 * {@code onto.graph.store-type: memory} (the default).
 */
public class InMemoryPopulationStore implements PopulationStore {

    private final Map<String, DecisionPopulation> populations = new HashMap<>();
    private final Map<String, Integer> feedbackCounters = new ConcurrentHashMap<>();
    private final List<EvolTrace> traces = Collections.synchronizedList(new ArrayList<>());

    @Override
    public DecisionPopulation findOrCreatePopulation(Concept concept, int maxSize) {
        return populations.computeIfAbsent(concept.getIri(),
                k -> new DecisionPopulation(concept, maxSize));
    }

    @Override
    public Optional<DecisionPopulation> getPopulation(String conceptIri) {
        return Optional.ofNullable(populations.get(conceptIri));
    }

    @Override
    public Map<String, DecisionPopulation> getAllPopulations() {
        return Collections.unmodifiableMap(populations);
    }

    @Override
    public void addMember(Concept concept, Assignment assignment) {
        DecisionPopulation pop = populations.get(concept.getIri());
        if (pop != null) {
            pop.addMember(assignment);
        }
    }

    @Override
    public void replaceMembers(Concept concept, List<Assignment> members) {
        DecisionPopulation pop = populations.get(concept.getIri());
        if (pop != null) {
            pop.replaceMembers(members);
        }
    }

    @Override
    public int incrementGeneration(Concept concept) {
        DecisionPopulation pop = populations.get(concept.getIri());
        if (pop != null) {
            pop.incrementGeneration();
            return pop.getGenerationCounter();
        }
        return 0;
    }

    @Override
    public int getGeneration(Concept concept) {
        DecisionPopulation pop = populations.get(concept.getIri());
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
    }

    @Override
    public List<EvolTrace> getTraces() {
        return Collections.unmodifiableList(traces);
    }

    @Override
    public void clear() {
        populations.clear();
        feedbackCounters.clear();
        traces.clear();
    }
}
