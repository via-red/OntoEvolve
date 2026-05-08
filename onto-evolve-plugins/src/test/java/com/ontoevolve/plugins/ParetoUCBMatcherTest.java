package com.ontoevolve.plugins;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.spi.Matcher;
import com.ontoevolve.plugins.matcher.ParetoUCBMatcher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParetoUCBMatcherTest {

    private final Concept concept = new Concept("http://test#C", "TestConcept");
    private final Decision decision = new Decision("d:1", "D1", "", List.of());
    private final Matcher.Context ctx = new Matcher.Context("subject1", Map.of());

    private Assignment makeAssignment(String iri, int trials, double... scores) {
        Assignment a = new Assignment(iri, decision, concept, scores.length);
        a.setStatus(Assignment.Status.ACTIVE);
        // Use updateScore to set both scores and trial count
        for (int i = 0; i < trials; i++) {
            a.updateScore(scores);
        }
        return a;
    }

    @Test
    void shouldReturnNullForEmptyPopulation() {
        ParetoUCBMatcher matcher = new ParetoUCBMatcher();
        assertNull(matcher.match(concept, ctx, List.of()));
    }

    @Test
    void shouldReturnNullForNullPopulation() {
        ParetoUCBMatcher matcher = new ParetoUCBMatcher();
        assertNull(matcher.match(concept, ctx, null));
    }

    @Test
    void shouldReturnOnlyMemberForSingleCandidate() {
        ParetoUCBMatcher matcher = new ParetoUCBMatcher();
        Assignment a = makeAssignment("a:1", 1, 0.8);
        Assignment result = matcher.match(concept, ctx, List.of(a));
        assertSame(a, result);
    }

    @Test
    void shouldPreferHigherScore() {
        ParetoUCBMatcher matcher = new ParetoUCBMatcher(0.0); // No exploration bonus
        Assignment low = makeAssignment("a:low", 10, 0.3);
        Assignment high = makeAssignment("a:high", 10, 0.9);
        Assignment result = matcher.match(concept, ctx, List.of(low, high));
        assertSame(high, result);
    }

    @Test
    void explorationBonusShouldHelpLessTestedCandidates() {
        ParetoUCBMatcher matcher = new ParetoUCBMatcher(0.5); // High exploration bonus
        Assignment wellTested = makeAssignment("a:tested", 100, 0.7);
        Assignment underTested = makeAssignment("a:new", 2, 0.65);
        Assignment result = matcher.match(concept, ctx, List.of(wellTested, underTested));
        assertSame(underTested, result, "UCB bonus should prefer the less-tested candidate");
    }

    @Test
    void defaultConstructorShouldCreateWorkingMatcher() {
        ParetoUCBMatcher matcher = new ParetoUCBMatcher();
        Assignment a = makeAssignment("a:1", 5, 0.7);
        assertNotNull(matcher.match(concept, ctx, List.of(a)));
    }
}
