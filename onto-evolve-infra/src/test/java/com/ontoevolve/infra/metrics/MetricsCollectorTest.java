package com.ontoevolve.infra.metrics;

import com.ontoevolve.core.kernel.DecisionPopulation;
import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.spi.GlobalMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCollectorTest {

    private MetricsCollector metrics;

    @BeforeEach
    void setUp() {
        metrics = new MetricsCollector();
    }

    @Test
    void shouldStartAtZero() {
        assertEquals(0, metrics.getLlmCalls());
        assertEquals(0, metrics.getTotalFeedbacks());
        assertEquals(0, metrics.getEvolutionRuns());
        assertEquals(0, metrics.getPopulationSize("http://test#Any"));
    }

    @Test
    void shouldRecordPopulationSize() {
        metrics.recordPopulationSize("http://test#C1", 5);
        assertEquals(5, metrics.getPopulationSize("http://test#C1"));
    }

    @Test
    void shouldRecordFeedback() {
        metrics.recordFeedback();
        metrics.recordFeedback();
        metrics.recordFeedback();
        assertEquals(3, metrics.getTotalFeedbacks());
    }

    @Test
    void shouldRecordLlmCalls() {
        metrics.recordLlmCall();
        metrics.recordLlmCall();
        assertEquals(2, metrics.getLlmCalls());
    }

    @Test
    void shouldRecordEvolutionRuns() {
        metrics.recordEvolutionRun();
        assertEquals(1, metrics.getEvolutionRuns());
    }

    @Test
    void shouldComputeAverageHypervolume() {
        assertEquals(0.0, metrics.getAverageHypervolume(), 1e-9);
        metrics.recordHypervolume(0.5);
        metrics.recordHypervolume(0.7);
        assertEquals(0.6, metrics.getAverageHypervolume(), 1e-9);
    }

    @Test
    void shouldLimitHypervolumeHistory() {
        for (int i = 0; i < 2000; i++) {
            metrics.recordHypervolume(i / 1000.0);
        }
        assertTrue(metrics.getAverageHypervolume() > 0);
    }

    @Test
    void shouldComputeNicheDiversityIndex() {
        Concept c1 = new Concept("http://test#C1", "C1");
        Concept c2 = new Concept("http://test#C2", "C2");
        Decision d = new Decision("d:1", "D1", "", List.of());

        DecisionPopulation pop1 = new DecisionPopulation(c1, 10);
        DecisionPopulation pop2 = new DecisionPopulation(c2, 10);

        Assignment a1 = new Assignment("a:1", d, c1, 2);
        a1.setStatus(Assignment.Status.ACTIVE);
        Assignment a2 = new Assignment("a:2", d, c2, 2);
        a2.setStatus(Assignment.Status.ACTIVE);

        pop1.addMember(a1);
        pop2.addMember(a2);

        Map<String, DecisionPopulation> pops = Map.of(
                "http://test#C1", pop1,
                "http://test#C2", pop2
        );

        double diversity = metrics.getNicheDiversityIndex(pops);
        assertTrue(diversity > 0, "Two equal populations should have positive diversity");
    }

    @Test
    void shouldReturnZeroDiversityForEmptyPopulations() {
        assertEquals(0.0, metrics.getNicheDiversityIndex(Map.of()), 1e-9);
    }

    @Test
    void shouldSnapshotGlobalMetrics() {
        metrics.recordFeedback();
        metrics.recordLlmCall();
        metrics.recordEvolutionRun();
        metrics.recordPopulationSize("http://test#C1", 3);

        Concept c1 = new Concept("http://test#C1", "C1");
        DecisionPopulation pop = new DecisionPopulation(c1, 10);
        Assignment a = new Assignment("a:1", new Decision("d:1", "D1", "", List.of()), c1, 2);
        a.setStatus(Assignment.Status.ACTIVE);
        pop.addMember(a);

        GlobalMetrics snapshot = metrics.snapshot(Map.of("http://test#C1", pop));
        assertNotNull(snapshot);
        assertTrue(snapshot.getTotalFeedback() > 0);
        assertTrue(snapshot.getLlmCallCost() > 0);
        assertTrue(snapshot.getTotalPopulations() > 0);
    }
}
