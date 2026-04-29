package com.ontoevolve.domain.education.model;

import com.ontoevolve.core.model.Execution;
import com.ontoevolve.core.model.Feedback;

import java.util.Map;

/**
 * 干预效果评价 — 教育领域 Feedback 子类。
 * <p>
 * 多维评价向量：
 * - effectiveness: 行为改善度 [0,1]
 * - cost: 执行成本 (越低越好，取 1-cost 存储)
 * - satisfaction: 学生/家长满意度 [0,1]
 * - longTerm: 长期效果（延迟到达，默认 0）
 */
public class Evaluation extends Feedback {
    public Evaluation(String iri, Execution execution,
                      double effectiveness, double cost, double satisfaction) {
        super(iri, execution,
              new double[]{effectiveness, 1 - cost, satisfaction, 0},
              Map.of("effectiveness", effectiveness,
                     "cost", cost,
                     "satisfaction", satisfaction));
    }

    public double getEffectiveness() { return getScores()[0]; }
    public double getCost() { return 1 - getScores()[1]; }
    public double getSatisfaction() { return getScores()[2]; }
}
