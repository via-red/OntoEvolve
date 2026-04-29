package com.ontoevolve.core.model;

import java.time.Instant;
import java.util.Map;

/**
 * 反馈评价 — 驱动进化的选择压力。
 * <p>
 * 每个 Feedback 包含多维向量（如短期效果、长期效果、成本、满意度等）
 * 以及上下文属性。这些维度在领域本体中定义，框架不做硬编码假设。
 */
public class Feedback {
    private final String iri;
    private final Execution execution;
    private final double[] scores;                  // 多维奖励向量
    private final Map<String, Object> attributes;   // 扩展属性
    private final Instant timestamp;

    public Feedback(String iri, Execution execution, double[] scores,
                    Map<String, Object> attributes) {
        this.iri = iri;
        this.execution = execution;
        this.scores = scores.clone();
        this.attributes = attributes;
        this.timestamp = Instant.now();
    }

    public String getIri() { return iri; }
    public Execution getExecution() { return execution; }
    public double[] getScores() { return scores.clone(); }
    public Map<String, Object> getAttributes() { return attributes; }
    public Instant getTimestamp() { return timestamp; }
    public int getDimension() { return scores.length; }
}
