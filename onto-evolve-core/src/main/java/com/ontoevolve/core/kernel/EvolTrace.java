package com.ontoevolve.core.kernel;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Decision;

import java.time.Instant;
import java.util.List;

/**
 * 进化轨迹 — 记录每一次变异操作的元数据。
 * <p>
 * 形成完整的方案谱系树，支撑可解释性与审计。
 * 每一个新 Assignment 的产生都应记录一条 EvolTrace。
 */
public class EvolTrace {
    public enum OperationType {
        LLM_GENERATE,   // LLM 全新生成
        CROSSOVER,      // 亲本重组
        PERTURB,        // 参数微调
        MIGRATE         // 跨生态位迁移
    }

    private final String id;
    private final OperationType operationType;
    private final List<Assignment> parentAssignments;
    private final Assignment producedAssignment;
    private final Decision producedDecision;
    private final String contextDescription;
    private final Instant timestamp;

    public EvolTrace(String id, OperationType operationType,
                     List<Assignment> parentAssignments,
                     Assignment producedAssignment,
                     Decision producedDecision,
                     String contextDescription) {
        this.id = id;
        this.operationType = operationType;
        this.parentAssignments = parentAssignments;
        this.producedAssignment = producedAssignment;
        this.producedDecision = producedDecision;
        this.contextDescription = contextDescription;
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public OperationType getOperationType() { return operationType; }
    public List<Assignment> getParentAssignments() { return parentAssignments; }
    public Assignment getProducedAssignment() { return producedAssignment; }
    public Decision getProducedDecision() { return producedDecision; }
    public String getContextDescription() { return contextDescription; }
    public Instant getTimestamp() { return timestamp; }
}
