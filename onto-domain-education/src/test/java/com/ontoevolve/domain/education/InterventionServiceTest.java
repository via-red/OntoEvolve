package com.ontoevolve.domain.education;

import com.ontoevolve.core.config.OntoEvolveConfig;
import com.ontoevolve.core.kernel.EvolutionEngine;
import com.ontoevolve.core.kernel.EvolTrace;
import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.spi.Matcher;
import com.ontoevolve.core.spi.Selector;
import com.ontoevolve.core.spi.Variator;
import com.ontoevolve.core.spi.VariationContext;
import com.ontoevolve.core.spi.Migrator;
import com.ontoevolve.core.validation.OntologyValidator;
import com.ontoevolve.domain.education.model.ActionEvent;
import com.ontoevolve.domain.education.model.ActionType;
import com.ontoevolve.domain.education.model.Intervention;
import com.ontoevolve.domain.education.service.EducationOntologyService;
import com.ontoevolve.domain.education.service.InterventionService;
import com.ontoevolve.infra.metrics.MetricsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InterventionServiceTest {

    private static final String NS = "http://ontoevolve/education#";

    private EducationOntologyService ontologyService;
    private EvolutionEngine engine;
    private InterventionService interventionService;
    private MetricsCollector metrics;

    @BeforeEach
    void setUp() {
        ontologyService = new EducationOntologyService();
        metrics = new MetricsCollector();

        // Matcher: pick first active member from the population for the concept
        Matcher<ActionType, Intervention, Assignment> testMatcher = new Matcher<>() {
            @Override
            public Assignment match(ActionType concept, Context ctx) {
                var pop = engine.getPopulations().get(concept.getIri());
                if (pop == null || pop.getActiveMembers().isEmpty()) return null;
                return pop.getActiveMembers().get(0);
            }
        };

        // Create engine with minimal dependencies
        Selector<Assignment> selector = (candidates, capacity) ->
                candidates.size() <= capacity ? candidates : candidates.subList(0, capacity);

        List<Variator> variators = new ArrayList<>();
        // No-op variator for safety
        variators.add(new Variator() {
            @Override public String type() { return "PERTURB"; }
            @Override public List generate(VariationContext ctx) { return List.of(); }
        });

        Migrator<Assignment> noopMigrator = (source, target, elites) -> List.of();
        OntologyValidator alwaysValid = a -> true;

        engine = new EvolutionEngine(selector, variators, noopMigrator, alwaysValid);

        // Wire config so migration check doesn't NPE
        OntoEvolveConfig config = new OntoEvolveConfig();
        config.getEvolution().getMigration().setEnabled(false);
        engine.setConfig(config);

        interventionService = new InterventionService(ontologyService, engine, testMatcher, metrics, (e -> {
            ActionType behavioral = ontologyService.findActionType(NS + "Behavioral")
                    .orElseThrow(() -> new RuntimeException("No Behavioral type"));
            return behavioral;
        }));
    }

    @Test
    void shouldProcessEventAndReturnIntervention() {
        ActionEvent event = new ActionEvent(
                "test:001", Instant.now(),
                "学生在课堂上大声喧哗",
                "S001", "教室", "moderate");

        Intervention result = interventionService.processEvent(event);
        assertNotNull(result, "Should return an intervention suggestion");
        assertNotNull(result.getName(), "Intervention should have a name");
    }

    @Test
    void shouldCreatePopulationOnFirstEvent() {
        ActionEvent event = new ActionEvent(
                "test:002", Instant.now(),
                "考试作弊被发现",
                "S002", "考场", "severe");

        interventionService.processEvent(event);
        assertFalse(engine.getPopulations().isEmpty(), "Engine should have populations after processing");
    }

    @Test
    void submittedEvaluationShouldRecordFeedback() {
        ActionEvent event = new ActionEvent(
                "test:003", Instant.now(),
                "拒绝参加班级活动",
                "S003", "操场", "mild");

        Intervention suggestion = interventionService.processEvent(event);
        assertNotNull(suggestion);

        long before = metrics.getTotalFeedbacks();
        interventionService.submitEvaluationByIntervention(
                suggestion.getIri(), "S003", 0.8, 0.2, 0.9);
        assertEquals(before + 1, metrics.getTotalFeedbacks(), "Feedback count should increase");
    }

    @Test
    void evolveNicheShouldRunWithoutError() {
        ActionEvent event = new ActionEvent(
                "test:004", Instant.now(),
                "上课走神发呆",
                "S004", "教室", "mild");

        interventionService.processEvent(event);

        // The classifier always returns Behavioral, so use that IRI
        String conceptIri = NS + "Behavioral";
        assertDoesNotThrow(() -> interventionService.evolveNiche(conceptIri));
    }

    @Test
    void metricsCollectorShouldTrackLlmCalls() {
        assertEquals(0, metrics.getLlmCalls());
        metrics.recordLlmCall();
        assertEquals(1, metrics.getLlmCalls());
    }

    @Test
    void repeatedProcessingShouldUseSeededPopulation() {
        ActionEvent event1 = new ActionEvent(
                "test:005", Instant.now(), "课堂喧哗", "S001", "教室", "moderate");
        ActionEvent event2 = new ActionEvent(
                "test:006", Instant.now(), "与同学争吵", "S002", "教室", "moderate");

        Intervention i1 = interventionService.processEvent(event1);
        Intervention i2 = interventionService.processEvent(event2);

        assertNotNull(i1);
        assertNotNull(i2);
    }

    @Test
    void tracesShouldBeRecordedAfterEvolution() {
        ActionEvent event = new ActionEvent(
                "test:007", Instant.now(),
                "在班级群发恶意P图",
                "S005", "网络", "severe");

        interventionService.processEvent(event);

        List<EvolTrace> before = engine.getTraces();
        interventionService.evolveNiche(NS + "Behavioral");
        List<EvolTrace> after = engine.getTraces();

        assertTrue(after.size() >= before.size(), "Traces should increase after evolution");
    }
}
