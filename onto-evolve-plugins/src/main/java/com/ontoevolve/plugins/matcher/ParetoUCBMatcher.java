package com.ontoevolve.plugins.matcher;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.spi.Matcher;

import java.util.Comparator;
import java.util.List;

/**
 * Pareto + UCB 混合匹配器。
 * <p>
 * 在 Pareto 前沿基础上叠加 UCB 探索奖励：
 * 对试验次数少的方案给予探索加分，平衡利用与探索。
 * 从种群中按综合得分选出最优匹配。
 */
public class ParetoUCBMatcher implements Matcher<Concept, Decision, Assignment> {

    private final double explorationBonus;

    public ParetoUCBMatcher(double explorationBonus) {
        this.explorationBonus = explorationBonus;
    }

    public ParetoUCBMatcher() {
        this(0.1);
    }

    @Override
    public Assignment match(Concept concept, Context ctx) {
        // 实际实现中应从 Concept 的种群中读取
        // 此处应由上层注入 DecisionPopulation
        throw new UnsupportedOperationException(
                "ParetoUCBMatcher 需要从 EvolutionEngine 获取 DecisionPopulation。" +
                "请使用 match(concept, ctx, population) 重载。");
    }

    /**
     * 从给定种群中匹配最优方案。
     */
    public Assignment match(Concept concept, Context ctx, List<Assignment> population) {
        if (population == null || population.isEmpty()) return null;

        int totalTrials = population.stream().mapToInt(Assignment::getTrials).sum();

        return population.stream()
                .max(Comparator.comparingDouble(a -> score(a, totalTrials)))
                .orElse(null);
    }

    /**
     * 混合分数 = Pareto 归一化得分 + UCB 探索奖励。
     * Pareto 归一化：将多维向量映射到 [0,1] 标量。
     * UCB 奖励：sqrt(2 * ln(N_total) / n_i)
     */
    private double score(Assignment a, int totalTrials) {
        double paretoScore = normalizeScore(a.getScoreVector());
        double ucbBonus = 0;
        if (a.getTrials() > 0 && totalTrials > 0) {
            ucbBonus = explorationBonus * Math.sqrt(2 * Math.log(totalTrials) / a.getTrials());
        }
        return paretoScore + ucbBonus;
    }

    private double normalizeScore(double[] scores) {
        if (scores.length == 0) return 0;
        double sum = 0;
        for (double s : scores) sum += s;
        return sum / scores.length;
    }
}
