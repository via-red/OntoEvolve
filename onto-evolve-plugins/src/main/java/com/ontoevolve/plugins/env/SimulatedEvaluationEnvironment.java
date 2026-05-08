package com.ontoevolve.plugins.env;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Execution;
import com.ontoevolve.core.model.Feedback;
import com.ontoevolve.core.spi.Environment;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 模拟评估环境 — 使用启发式规则和高斯噪声生成反馈。
 * <p>
 * 用于离线测试和种群初始化，无需人工评估。
 */
public class SimulatedEvaluationEnvironment implements Environment {

    private final Random random = new Random();
    private final double noiseSigma;
    private final double bias;

    public SimulatedEvaluationEnvironment(double noiseSigma, double bias) {
        this.noiseSigma = noiseSigma;
        this.bias = bias;
    }

    public SimulatedEvaluationEnvironment() {
        this(0.1, 0.5);
    }

    @Override
    public Feedback evaluate(Assignment assignment, Execution execution) {
        var decision = assignment.getDecision();

        // 启发式计算
        int stepsCount = decision.getSteps() != null ? decision.getSteps().size() : 3;
        int nameLength = decision.getName() != null ? decision.getName().length() : 5;

        // effectiveness: 步骤数多 → 方案更细致 → 基础分高
        double effectiveness = bias + 0.1 * Math.min(stepsCount, 5) + gaussianNoise();
        // cost: 步骤数多 → 成本高 → 分数低
        double cost = bias - 0.08 * Math.min(stepsCount, 5) + gaussianNoise();
        // satisfaction: 名字长度适中 → 描述清晰
        double satisfaction = bias + 0.05 * Math.min(nameLength, 10) + gaussianNoise();

        double[] scores = {
                clamp(effectiveness, 0, 1),
                clamp(cost, 0, 1),
                clamp(satisfaction, 0, 1)
        };

        return new Feedback(
                "sim-fb:" + UUID.randomUUID(),
                execution,
                scores,
                Map.of("source", "simulated", "noiseSigma", noiseSigma)
        );
    }

    @Override
    public boolean isAutomated() {
        return true;
    }

    private double gaussianNoise() {
        return random.nextGaussian() * noiseSigma;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
