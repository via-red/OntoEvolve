package com.ontoevolve.core.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * 方案分配 — 将 Decision 绑定到 Concept 的载体。
 * <p>
 * Assignment 是进化发生的实际载体，携带多维评分、代际、谱系引用和生命周期状态。
 * 每个 Assignment 是种群中的一个"个体"。
 */
public class Assignment {
    public enum Status {
        NEWBORN,      // 新生方案，待观察
        ACTIVE,       // 活跃方案，正常参与选择
        PROBATION,    // 观察期，表现不佳被降级
        DEPRECATED,   // 废弃方案，不再使用
        ELITE         // 精英方案，跨生态位迁移的候选种子
    }

    private final String iri;
    private final Decision decision;
    private final Concept concept;
    private double[] scoreVector;      // 多维反馈均值
    private double[] varianceVector;   // 多维反馈方差
    private int trials;                // 尝试次数
    private int generation;            // 代际编号
    private List<Assignment> parents;  // 亲本引用（重组时有两个）
    private Status status;
    private Instant lastUpdated;

    public Assignment(String iri, Decision decision, Concept concept, int dimension) {
        this.iri = iri;
        this.decision = decision;
        this.concept = concept;
        this.scoreVector = new double[dimension];
        this.varianceVector = new double[dimension];
        this.trials = 0;
        this.generation = 0;
        this.status = Status.NEWBORN;
        this.lastUpdated = Instant.now();
    }

    /** 在线更新多维统计量 (Welford 在线算法) */
    public void updateScore(double[] feedback) {
        if (feedback.length != scoreVector.length) {
            throw new IllegalArgumentException("Feedback dimension mismatch");
        }
        trials++;
        for (int i = 0; i < scoreVector.length; i++) {
            double delta = feedback[i] - scoreVector[i];
            scoreVector[i] += delta / trials;
            double delta2 = feedback[i] - scoreVector[i];
            varianceVector[i] += delta * delta2;
        }
        this.lastUpdated = Instant.now();
    }

    public double[] getScoreVector() { return scoreVector.clone(); }
    public double[] getVarianceVector() {
        double[] var = varianceVector.clone();
        for (int i = 0; i < var.length; i++) {
            var[i] = trials > 1 ? var[i] / (trials - 1) : 1.0;
        }
        return var;
    }
    public int getTrials() { return trials; }
    public int getGeneration() { return generation; }
    public void setGeneration(int generation) { this.generation = generation; }
    public List<Assignment> getParents() { return parents; }
    public void setParents(List<Assignment> parents) { this.parents = parents; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getIri() { return iri; }
    public Decision getDecision() { return decision; }
    public Concept getConcept() { return concept; }
    public Instant getLastUpdated() { return lastUpdated; }
}
