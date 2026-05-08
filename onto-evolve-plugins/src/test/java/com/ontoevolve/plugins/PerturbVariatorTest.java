package com.ontoevolve.plugins;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.spi.VariationContext;
import com.ontoevolve.plugins.variator.PerturbVariator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PerturbVariatorTest {

    private final Concept concept = new Concept("http://test#C", "TestConcept");

    private Assignment makeAssignment(String iri, String decisionIri, String name,
                                      List<String> steps, double... scores) {
        Decision d = new Decision(decisionIri, name, "", steps);
        Assignment a = new Assignment(iri, d, concept, scores.length);
        a.updateScore(scores);
        a.setStatus(Assignment.Status.ACTIVE);
        return a;
    }

    @Test
    void shouldReturnEmptyForEmptyPopulation() {
        PerturbVariator variator = new PerturbVariator();
        VariationContext ctx = new VariationContext(concept, List.of(), Map.of(), 0.1);
        assertTrue(variator.generate(ctx).isEmpty());
    }

    @Test
    void shouldCreateSingleChildFromParent() {
        PerturbVariator variator = new PerturbVariator();
        Assignment parent = makeAssignment("a:1", "d:1", "谈话",
                List.of("与学生谈话", "通知家长", "观察一周"), 0.8, 0.6);
        VariationContext ctx = new VariationContext(concept, List.of(parent), Map.of(), 0.1);

        List<Assignment> result = variator.generate(ctx);
        assertEquals(1, result.size());

        Assignment child = result.get(0);
        assertNotNull(child.getDecision());
        assertTrue(child.getDecision().getName().contains("微调"));
    }

    @Test
    void childShouldInheritParentWithModification() {
        PerturbVariator variator = new PerturbVariator();
        Assignment parent = makeAssignment("a:1", "d:1", "书面警告",
                List.of("记录违纪", "通知家长"), 0.7, 0.5);
        VariationContext ctx = new VariationContext(concept, List.of(parent), Map.of(), 0.1);

        List<Assignment> result = variator.generate(ctx);
        Assignment child = result.get(0);

        Decision childDecision = child.getDecision();
        assertEquals(1, childDecision.getParents().size());
        assertTrue(child.getGeneration() > parent.getGeneration());
    }

    @Test
    void shouldReturnCorrectType() {
        PerturbVariator variator = new PerturbVariator();
        assertEquals("PERTURB", variator.type());
    }

    @Test
    void shouldPerturbNumericSteps() {
        PerturbVariator variator = new PerturbVariator();
        Assignment parent = makeAssignment("a:1", "d:1", "定时提醒",
                List.of("每2小时提醒一次"), 0.9);
        VariationContext ctx = new VariationContext(concept, List.of(parent), Map.of(), 0.1);

        List<Assignment> result = variator.generate(ctx);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getDecision());
    }

    @Test
    void childAssignmentShouldHaveCorrectScoreDimension() {
        PerturbVariator variator = new PerturbVariator();
        Assignment parent = makeAssignment("a:1", "d:1", "评估",
                List.of("面谈"), 0.8, 0.7, 0.9);
        VariationContext ctx = new VariationContext(concept, List.of(parent), Map.of(), 0.1);

        List<Assignment> result = variator.generate(ctx);
        Assignment child = result.get(0);
        assertEquals(3, child.getScoreVector().length,
                "Child should have same score dimension as parent");
    }
}
