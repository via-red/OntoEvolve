package com.ontoevolve.plugins.meta;

import com.ontoevolve.core.spi.GlobalMetrics;
import com.ontoevolve.core.spi.MetaOptimizer;
import com.ontoevolve.infra.metrics.MetricsCollector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 贝叶斯元优化器 — 以系统全局指标（含多样性）为目标调整进化超参数。
 * <p>
 * 目标函数: w1 * avgHypervolume + w2 * nicheDiversity - w3 * llmCallCost
 * 使用简化爬山法 + 随机扰动近似贝叶斯优化。
 */
public class BayesianMetaOptimizer implements MetaOptimizer {

    private final Random random = new Random();
    private Map<String, Double> currentParams;
    private double bestMetric = Double.NEGATIVE_INFINITY;
    private int stagnationCount = 0;

    private final double wHypervolume;
    private final double wDiversity;
    private final double wCost;

    private MetricsCollector metricsCollector;

    public BayesianMetaOptimizer() {
        this(0.5, 0.3, 0.2);
    }

    public BayesianMetaOptimizer(double wHypervolume, double wDiversity, double wCost) {
        this.wHypervolume = wHypervolume;
        this.wDiversity = wDiversity;
        this.wCost = wCost;
        this.currentParams = new HashMap<>();
        currentParams.put("explorationRate", 0.15);
        currentParams.put("populationCapacity", 20.0);
        currentParams.put("crowdingThreshold", 0.1);
    }

    /** Setter 注入，可选。 */
    public void setMetricsCollector(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public Map<String, Double> optimize(GlobalMetrics metrics) {
        double currentMetric = wHypervolume * metrics.getAverageHypervolume()
                + wDiversity * metrics.getNicheDiversityIndex()
                - wCost * Math.log1p(metrics.getLlmCallCost());

        if (stagnationCount > 3 || currentMetric > bestMetric) {
            currentParams = hillClimb(currentParams);
            if (currentMetric > bestMetric) {
                bestMetric = currentMetric;
                stagnationCount = 0;
            } else {
                stagnationCount++;
            }
        } else {
            currentParams = randomPerturb(currentParams);
            stagnationCount++;
        }

        return new HashMap<>(currentParams);
    }

    private Map<String, Double> hillClimb(Map<String, Double> params) {
        Map<String, Double> newParams = new HashMap<>(params);
        String key = List.of("explorationRate", "populationCapacity")
                .get(random.nextInt(2));
        double delta = (random.nextDouble() - 0.5) * 0.1;
        double newValue = params.get(key) + delta;
        newValue = Math.max(0.01, Math.min(0.5, newValue));
        newParams.put(key, newValue);
        return newParams;
    }

    private Map<String, Double> randomPerturb(Map<String, Double> params) {
        Map<String, Double> newParams = new HashMap<>(params);
        for (String key : params.keySet()) {
            double noise = (random.nextDouble() - 0.5) * 0.2;
            double newValue = params.get(key) + noise;
            newValue = Math.max(0.01, Math.min(0.5, newValue));
            newParams.put(key, newValue);
        }
        return newParams;
    }

    public double getBestMetric() { return bestMetric; }
    public int getStagnationCount() { return stagnationCount; }
}
