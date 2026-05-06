package com.ontoevolve.core;

import com.ontoevolve.core.kernel.DecisionPopulation;
import com.ontoevolve.core.kernel.EvolTrace;
import com.ontoevolve.core.kernel.EvolutionEngine;
import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.spi.Selector;
import com.ontoevolve.core.spi.VariationContext;
import com.ontoevolve.core.spi.Variator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EvolutionEngine 单元测试。
 */
class EvolutionEngineTest {

    private EvolutionEngine engine;
    private Concept concept;
    private Decision decision;

    @BeforeEach
    void setUp() {
        Selector<Assignment> selector = new Selector<>() {
            @Override
            public List<Assignment> select(List<Assignment> candidates, int capacity) {
                return candidates.size() <= capacity ? candidates
                        : candidates.subList(0, capacity);
            }
        };

        Variator<Decision, Assignment> variator = new Variator<>() {
            @Override
            public List<Assignment> generate(VariationContext ctx) {
                return List.of();
            }

            @Override
            public String type() {
                return "PERTURB";
            }
        };

        engine = new EvolutionEngine(
                selector, List.of(variator), null, assignment -> true);
        concept = new Concept("http://test#C1", "TestConcept");
        decision = new Decision("d:1", "D1", "", List.of("step1"));
    }

    @Test
    void shouldCreatePopulationOnDemand() {
        DecisionPopulation pop = engine.getOrCreatePopulation(concept, 10);
        assertNotNull(pop);
        assertEquals(concept, pop.getConcept());
        assertEquals(10, pop.getMaxSize());

        // 再次获取应返回相同实例
        DecisionPopulation same = engine.getOrCreatePopulation(concept, 20);
        assertSame(pop, same);
    }

    @Test
    void shouldTrackFeedbackCounters() {
        engine.getOrCreatePopulation(concept, 10);
        // 先添加一个成员，否则 isEvolutionDue 会因空种群而返回 false
        Assignment assignment = new Assignment("a:1", decision, concept, 2);
        engine.getOrCreatePopulation(concept, 10).addMember(assignment);

        engine.recordFeedback(concept);
        // 由于只记录了1条反馈且perNiche=20（默认），不会触发进化
        assertEquals(1, engine.getPopulations().size());
    }

    @Test
    void shouldRunFullEvolution() {
        engine.getOrCreatePopulation(concept, 10);
        Assignment a = new Assignment("a:1", decision, concept, 2);
        a.updateScore(new double[]{0.8, 0.6});
        a.setStatus(Assignment.Status.ACTIVE);
        engine.getOrCreatePopulation(concept, 10).addMember(a);

        engine.runFullEvolution(concept);
        assertEquals(1, getGeneration(concept));
    }

    private int getGeneration(Concept c) {
        return engine.getOrCreatePopulation(c, 10).getGenerationCounter();
    }

    @Test
    void shouldRecordEvolTraces() {
        assertTrue(engine.getTraces().isEmpty());

        engine.addTrace(new EvolTrace(
                "trace:1", EvolTrace.OperationType.PERTURB,
                List.of(), null, decision, "test trace"));

        assertFalse(engine.getTraces().isEmpty());
        assertEquals(1, engine.getTraces().size());
        assertEquals("trace:1", engine.getTraces().get(0).getId());
    }
}
