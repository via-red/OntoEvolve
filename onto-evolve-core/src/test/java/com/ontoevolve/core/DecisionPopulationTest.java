package com.ontoevolve.core;

import com.ontoevolve.core.kernel.DecisionPopulation;
import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DecisionPopulation 单元测试。
 */
class DecisionPopulationTest {

    @Test
    void shouldStartEmpty() {
        Concept concept = new Concept("http://test#C1", "C1");
        DecisionPopulation pop = new DecisionPopulation(concept, 10);

        assertEquals(0, pop.size());
        assertTrue(pop.getActiveMembers().isEmpty());
    }

    @Test
    void shouldAddMemberWithGeneration() {
        Concept concept = new Concept("http://test#C1", "C1");
        DecisionPopulation pop = new DecisionPopulation(concept, 10);
        Decision d = new Decision("d:1", "D1", "", List.of());
        Assignment a = new Assignment("a:1", d, concept, 2);

        pop.addMember(a);

        assertEquals(1, pop.size());
        assertEquals(0, a.getGeneration()); // 初始代 = 0
        assertEquals(Assignment.Status.NEWBORN, a.getStatus());
    }

    @Test
    void shouldTrackGenerationCounter() {
        Concept concept = new Concept("http://test#C1", "C1");
        DecisionPopulation pop = new DecisionPopulation(concept, 10);

        assertEquals(0, pop.getGenerationCounter());
        pop.incrementGeneration();
        assertEquals(1, pop.getGenerationCounter());
        pop.incrementGeneration();
        assertEquals(2, pop.getGenerationCounter());
    }

    @Test
    void shouldReplaceMembers() {
        Concept concept = new Concept("http://test#C1", "C1");
        DecisionPopulation pop = new DecisionPopulation(concept, 10);
        Decision d = new Decision("d:1", "D1", "", List.of());
        Assignment a1 = new Assignment("a:1", d, concept, 2);
        Assignment a2 = new Assignment("a:2", d, concept, 2);

        pop.addMember(a1);
        assertEquals(1, pop.size());

        pop.replaceMembers(List.of(a2));
        assertEquals(1, pop.size());
        assertEquals(a2, pop.getActiveMembers().get(0));
    }

    @Test
    void shouldDetectEvolutionDue() {
        Concept concept = new Concept("http://test#C1", "C1");
        DecisionPopulation pop = new DecisionPopulation(concept, 10);
        Decision d = new Decision("d:1", "D1", "", List.of());

        // 空种群不应触发进化
        assertFalse(pop.isEvolutionDue(10));

        pop.addMember(new Assignment("a:1", d, concept, 2));
        assertTrue(pop.isEvolutionDue(10)); // feedbackCount >= maxSize && !empty
        assertFalse(pop.isEvolutionDue(5));  // feedbackCount < maxSize
    }
}
