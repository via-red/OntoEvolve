package com.ontoevolve.plugins.meta;

import com.ontoevolve.core.spi.GlobalMetrics;
import com.ontoevolve.core.spi.MetaOptimizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 贝叶斯元优化器 — 以系统全局指标为目标调整进化超参数。
 * <p>
 * 工程简化版：使用随机采样 + 爬山法近似贝叶斯优化。
 * 生产环境可替换为基于 GPyTorch 或 SMAC 的真正贝叶斯优化。
 */
public class BayesianMetaOptimizer implements MetaOptimizer {

    private final Random random = new Random();
    private Map<String, Double> currentParams;
    private double bestMetric = Double.NEGATIVE_INFINITY;
    private int stagnationCount = 0;

    public BayesianMetaOptimizer() {
        this.currentParams = new HashMap<>();
        currentParams.put("explorationRate", 0.15);
        currentParams.put("populationCapacity", 20.0);
        currentParams.put("crowdingThreshold", 0.1);
    }

    @Override
    public Map<String, Double> optimize(GlobalMetrics metrics) {
        double currentMetric = metrics.getAverageHypervolume()
                - 0.1 * metrics.getLlmCallCost();

        // 勘探阶段
        if (stagnationCount > 3 || currentMetric > bestMetric) {
            currentParams = hillClimb(currentParams, currentMetric);
            if (currentMetric > bestMetric) {
                bestMetric = currentMetric;
                stagnationCount = 0;
            } else {
                stagnationCount++;
            }
        } else {
            // 如果停滞，随机扰动
            currentParams = randomPerturb(currentParams);
            stagnationCount++;
        }

        return new HashMap<>(currentParams);
    }

    private Map<String, Double> hillClimb(Map<String, Double> params, double metric) {
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
}
