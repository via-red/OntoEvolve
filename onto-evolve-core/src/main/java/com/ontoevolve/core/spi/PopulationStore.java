package com.ontoevolve.core.spi;

import com.ontoevolve.core.kernel.DecisionPopulation;
import com.ontoevolve.core.kernel.EvolTrace;
import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SPI for population storage — decouples EvolutionEngine from persistence.
 *
 * <p>Default in-memory implementation is provided. A Neo4j-backed implementation
 * can be swapped in via {@code onto.graph.store-type: neo4j}.
 */
public interface PopulationStore {

    DecisionPopulation findOrCreatePopulation(Concept concept, int maxSize);

    Optional<DecisionPopulation> getPopulation(String conceptIri);

    Map<String, DecisionPopulation> getAllPopulations();

    void addMember(Concept concept, Assignment assignment);

    void replaceMembers(Concept concept, List<Assignment> members);

    int incrementGeneration(Concept concept);

    int getGeneration(Concept concept);

    int incrementFeedbackCounter(Concept concept);

    int getFeedbackCount(Concept concept);

    void resetFeedbackCounter(Concept concept);

    void addTrace(EvolTrace trace);

    List<EvolTrace> getTraces();

    void clear();
}
