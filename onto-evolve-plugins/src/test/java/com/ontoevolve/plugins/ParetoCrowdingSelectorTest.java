package com.ontoevolve.plugins;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.plugins.selector.ParetoCrowdingSelector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ParetoCrowdingSelector 单元测试。
 */
class ParetoCrowdingSelectorTest {

    private final Concept concept = new Concept("http://test#C1", "C1");
    private final Decision d1 = new Decision("d:1", "D1", "", List.of());

    private Assignment makeAssignment(String iri, double... scores) {
        Assignment a = new Assignment(iri, d1, concept, scores.length);
        a.updateScore(scores);
        a.setStatus(Assignment.Status.ACTIVE);
        return a;
    }

    @Test
    void shouldReturnEmptyForEmptyInput() {
        ParetoCrowdingSelector selector = new ParetoCrowdingSelector();
        assertTrue(selector.select(List.of(), 10).isEmpty());
    }

    @Test
    void shouldReturnAllIfUnderCapacity() {
        ParetoCrowdingSelector selector = new ParetoCrowdingSelector();
        List<Assignment> candidates = List.of(
                makeAssignment("a:1", 0.8, 0.6),
                makeAssignment("a:2", 0.7, 0.5)
        );

        List<Assignment> selected = selector.select(candidates, 10);
        assertEquals(2, selected.size());
    }

    @Test
    void shouldSelectNonDominatedFront() {
        ParetoCrowdingSelector selector = new ParetoCrowdingSelector();
        Assignment a = makeAssignment("a:1", 0.9, 0.8);
        Assignment b = makeAssignment("a:2", 0.3, 0.4);
        Assignment c = makeAssignment("a:3", 0.7, 0.9);

        List<Assignment> candidates = List.of(a, b, c);
        List<Assignment> selected = selector.select(candidates, 2);

        assertEquals(2, selected.size());
        assertFalse(selected.contains(b));
    }

    @Test
    void shouldKeepDiversityThroughCrowdingDistance() {
        ParetoCrowdingSelector selector = new ParetoCrowdingSelector();
        List<Assignment> candidates = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            double v = 0.1 * (i + 1);
            candidates.add(makeAssignment("a:" + i, v, 1.0 - v));
        }

        List<Assignment> selected = selector.select(candidates, 3);
        assertEquals(3, selected.size());
        assertTrue(selected.contains(candidates.get(0)));
        assertTrue(selected.contains(candidates.get(4)));
    }

    @Test
    void shouldHandleSingleDimension() {
        ParetoCrowdingSelector selector = new ParetoCrowdingSelector();
        List<Assignment> candidates = List.of(
                makeAssignment("a:1", 0.5),
                makeAssignment("a:2", 0.8),
                makeAssignment("a:3", 0.3)
        );

        List<Assignment> selected = selector.select(candidates, 2);
        assertEquals(2, selected.size());
    }
}
