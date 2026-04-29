package com.ontoevolve.core.spi;

import java.util.Map;

/**
 * 全局系统指标快照 — 供 MetaOptimizer 使用。
 * <p>
 * 包含所有生态位的宏观统计数据，反映系统的整体进化健康状况。
 */
public class GlobalMetrics {
    private final double averageHypervolume;      // 平均 Pareto 前沿超体积
    private final double nicheDiversityIndex;     // 多样性指数 (Shannon)
    private final double deprecationRate;         // 方案废弃率
    private final double llmCallCost;             // LLM 调用成本
    private final int totalPopulations;           // 生态位总数
    private final long totalFeedback;             // 累积反馈数
    private final Map<String, Double> extraMetrics;

    public GlobalMetrics(double averageHypervolume, double nicheDiversityIndex,
                         double deprecationRate, double llmCallCost,
                         int totalPopulations, long totalFeedback,
                         Map<String, Double> extraMetrics) {
        this.averageHypervolume = averageHypervolume;
        this.nicheDiversityIndex = nicheDiversityIndex;
        this.deprecationRate = deprecationRate;
        this.llmCallCost = llmCallCost;
        this.totalPopulations = totalPopulations;
        this.totalFeedback = totalFeedback;
        this.extraMetrics = extraMetrics;
    }

    public double getAverageHypervolume() { return averageHypervolume; }
    public double getNicheDiversityIndex() { return nicheDiversityIndex; }
    public double getDeprecationRate() { return deprecationRate; }
    public double getLlmCallCost() { return llmCallCost; }
    public int getTotalPopulations() { return totalPopulations; }
    public long getTotalFeedback() { return totalFeedback; }
    public Map<String, Double> getExtraMetrics() { return extraMetrics; }
}
