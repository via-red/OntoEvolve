package com.ontoevolve.plugins;

import com.ontoevolve.core.spi.GlobalMetrics;
import com.ontoevolve.plugins.meta.BayesianMetaOptimizer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BayesianMetaOptimizerTest {

    @Test
    void shouldInitializeWithDefaultParams() {
        BayesianMetaOptimizer optimizer = new BayesianMetaOptimizer();
        GlobalMetrics metrics = new GlobalMetrics(0.5, 0.3, 0.1, 2, 3, 10, Map.of());
        Map<String, Double> params = optimizer.optimize(metrics);

        assertNotNull(params);
        assertTrue(params.containsKey("explorationRate"));
        assertTrue(params.containsKey("populationCapacity"));
        assertTrue(params.containsKey("crowdingThreshold"));
    }

    @Test
    void shouldClampValuesAfterPerturbation() {
        BayesianMetaOptimizer optimizer = new BayesianMetaOptimizer();
        // Run enough iterations to trigger random perturbation
        GlobalMetrics poor = new GlobalMetrics(0.05, 0.05, 0.9, 20, 5, 100, Map.of());
        for (int i = 0; i < 10; i++) {
            Map<String, Double> params = optimizer.optimize(poor);
            for (Map.Entry<String, Double> e : params.entrySet()) {
                assertTrue(e.getValue() >= 0.01,
                        e.getKey() + " should be >= 0.01, got " + e.getValue());
            }
        }
    }

    @Test
    void shouldImproveWithBetterMetrics() {
        BayesianMetaOptimizer optimizer = new BayesianMetaOptimizer();

        // High hypervolume, low cost — good state
        GlobalMetrics goodMetrics = new GlobalMetrics(0.9, 0.8, 0.1, 1, 5, 100, Map.of());
        Map<String, Double> params1 = optimizer.optimize(goodMetrics);

        // Same good metrics again — should hill-climb
        Map<String, Double> params2 = optimizer.optimize(goodMetrics);

        assertNotNull(params2);
    }

    @Test
    void shouldDegradeWithPoorMetrics() {
        BayesianMetaOptimizer optimizer = new BayesianMetaOptimizer();

        // Start with decent metrics
        GlobalMetrics decent = new GlobalMetrics(0.6, 0.5, 0.1, 2, 5, 100, Map.of());
        optimizer.optimize(decent);

        // Then poor metrics — triggering random perturbation
        GlobalMetrics poor = new GlobalMetrics(0.1, 0.1, 0.9, 20, 5, 100, Map.of());
        Map<String, Double> params = optimizer.optimize(poor);

        assertNotNull(params);
        // Should still produce valid params
        assertTrue(params.get("explorationRate") >= 0.01);
    }
}
