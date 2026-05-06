package com.ontoevolve.plugins.credit;

import com.ontoevolve.core.model.Execution;
import com.ontoevolve.core.model.Feedback;
import com.ontoevolve.core.spi.CreditAssigner;

import java.util.ArrayList;
import java.util.List;

/**
 * 均匀信用分配器 — 将延迟到达的长期反馈沿时间窗口均匀分配。
 * <p>
 * 当长期反馈（如学期末成绩）到达时，回溯关联的历史 Execution，
 * 按时间衰减系数 λ 将奖励分配到历史执行记录上。
 * 适用于教育领域：期末成绩反映了一学期所有干预的综合效果。
 */
public class UniformCreditAssigner implements CreditAssigner {

    private final double lambda;
    private final int maxLookback;

    public UniformCreditAssigner(double lambda, int maxLookback) {
        this.lambda = lambda;
        this.maxLookback = maxLookback;
    }

    public UniformCreditAssigner() {
        this(0.9, 30);
    }

    @Override
    public List<Feedback> distribute(Feedback feedback, List<Execution> history) {
        List<Feedback> derived = new ArrayList<>();
        if (history.isEmpty()) {
            derived.add(feedback);
            return derived;
        }

        // 只取最近的 maxLookback 条记录
        List<Execution> window = history.size() > maxLookback
                ? history.subList(0, maxLookback)
                : history;

        double totalWeight = 0.0;
        double[] weights = new double[window.size()];
        for (int i = 0; i < window.size(); i++) {
            // 越近的权重越高（指数衰减）
            weights[i] = Math.pow(lambda, i);
            totalWeight += weights[i];
        }

        double[] originalScores = feedback.getScores();
        for (int i = 0; i < window.size(); i++) {
            Execution exec = window.get(i);
            double proportion = weights[i] / totalWeight;
            double[] allocated = new double[originalScores.length];
            for (int d = 0; d < originalScores.length; d++) {
                allocated[d] = originalScores[d] * proportion;
            }

            derived.add(new Feedback(
                    feedback.getIri() + "-derived-" + i,
                    exec, allocated, feedback.getAttributes()
            ));
        }

        return derived;
    }
}
