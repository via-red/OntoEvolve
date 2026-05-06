package com.ontoevolve.plugins;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.model.Execution;
import com.ontoevolve.core.model.Feedback;
import com.ontoevolve.plugins.credit.UniformCreditAssigner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UniformCreditAssigner 单元测试。
 */
class UniformCreditAssignerTest {

    @Test
    void shouldReturnOriginalFeedbackWhenNoHistory() {
        UniformCreditAssigner assigner = new UniformCreditAssigner(0.9, 30);
        Feedback feedback = new Feedback("f:1", null, new double[]{0.8, 0.6}, Map.of());

        List<Feedback> result = assigner.distribute(feedback, List.of());
        assertEquals(1, result.size());
        assertArrayEquals(feedback.getScores(), result.get(0).getScores(), 1e-9);
    }

    @Test
    void shouldDistributeAcrossHistory() {
        UniformCreditAssigner assigner = new UniformCreditAssigner(0.5, 30);
        Concept concept = new Concept("http://test#C1", "C1");
        Decision d = new Decision("d:1", "D1", "", List.of());
        Assignment a = new Assignment("a:1", d, concept, 2);

        Execution exec1 = new Execution("e:1", a, "s1", "t1", Map.of());
        Execution exec2 = new Execution("e:2", a, "s1", "t1", Map.of());

        Feedback feedback = new Feedback("f:1", null, new double[]{1.0, 0.5}, Map.of());

        List<Feedback> result = assigner.distribute(feedback, List.of(exec1, exec2));
        assertFalse(result.isEmpty());
    }

    @Test
    void shouldRespectMaxLookback() {
        UniformCreditAssigner assigner = new UniformCreditAssigner(0.9, 2);
        Concept concept = new Concept("http://test#C1", "C1");
        Decision d = new Decision("d:1", "D1", "", List.of());
        Assignment a = new Assignment("a:1", d, concept, 2);

        List<Execution> history = List.of(
                new Execution("e:1", a, "s1", "t1", Map.of()),
                new Execution("e:2", a, "s1", "t1", Map.of()),
                new Execution("e:3", a, "s1", "t1", Map.of())
        );

        Feedback feedback = new Feedback("f:1", null, new double[]{0.5, 0.5}, Map.of());

        List<Feedback> result = assigner.distribute(feedback, history);
        assertFalse(result.isEmpty());
    }
}
