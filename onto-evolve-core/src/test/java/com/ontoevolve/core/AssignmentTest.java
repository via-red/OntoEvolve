package com.ontoevolve.core;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.model.Feedback;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Assignment 模型单元测试。
 */
class AssignmentTest {

    @Test
    void shouldInitializeWithZeroScores() {
        Concept concept = new Concept("http://test#C1", "TestConcept");
        Decision decision = new Decision("d:1", "TestDecision", "A test", List.of("step1"));
        Assignment assignment = new Assignment("a:1", decision, concept, 3);

        assertEquals(3, assignment.getScoreVector().length);
        assertEquals(0.0, assignment.getScoreVector()[0]);
        assertEquals(0, assignment.getTrials());
        assertEquals(Assignment.Status.NEWBORN, assignment.getStatus());
    }

    @Test
    void shouldUpdateScoreCorrectly() {
        Concept concept = new Concept("http://test#C1", "TestConcept");
        Decision decision = new Decision("d:1", "TestDecision", "", List.of());
        Assignment assignment = new Assignment("a:1", decision, concept, 2);

        assignment.updateScore(new double[]{0.8, 0.6});
        assertEquals(1, assignment.getTrials());
        assertEquals(0.8, assignment.getScoreVector()[0], 1e-9);
        assertEquals(0.6, assignment.getScoreVector()[1], 1e-9);

        assignment.updateScore(new double[]{0.4, 0.8});
        assertEquals(2, assignment.getTrials());
        assertEquals(0.6, assignment.getScoreVector()[0], 1e-9);
        assertEquals(0.7, assignment.getScoreVector()[1], 1e-9);
    }

    @Test
    void shouldThrowOnDimensionMismatch() {
        Concept concept = new Concept("http://test#C1", "TestConcept");
        Decision decision = new Decision("d:1", "TestDecision", "", List.of());
        Assignment assignment = new Assignment("a:1", decision, concept, 2);

        assertThrows(IllegalArgumentException.class,
                () -> assignment.updateScore(new double[]{0.5, 0.3, 0.1}));
    }

    @Test
    void shouldTrackGenerationAndStatus() {
        Concept concept = new Concept("http://test#C1", "TestConcept");
        Decision decision = new Decision("d:1", "TestDecision", "", List.of());
        Assignment assignment = new Assignment("a:1", decision, concept, 1);

        assertEquals(0, assignment.getGeneration());
        assignment.setGeneration(5);
        assertEquals(5, assignment.getGeneration());

        assignment.setStatus(Assignment.Status.ELITE);
        assertEquals(Assignment.Status.ELITE, assignment.getStatus());
    }
}
