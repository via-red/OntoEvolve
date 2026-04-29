package com.ontoevolve.core.model;

import java.time.Instant;
import java.util.Map;

/**
 * 方案执行记录 — 反馈的锚点。
 * <p>
 * 记录谁、在什么时间、为谁、在什么上下文中执行了哪个方案。
 * 每条 Execution 产生一条或多条 Feedback。
 */
public class Execution {
    private final String iri;
    private final Assignment assignment;
    private final String subjectId;        // 被执行对象（如学生ID）
    private final String executorId;       // 执行者（如教师ID）
    private final Instant executedAt;
    private final Map<String, Object> contextSnapshot;

    public Execution(String iri, Assignment assignment, String subjectId,
                     String executorId, Map<String, Object> contextSnapshot) {
        this.iri = iri;
        this.assignment = assignment;
        this.subjectId = subjectId;
        this.executorId = executorId;
        this.executedAt = Instant.now();
        this.contextSnapshot = contextSnapshot;
    }

    public String getIri() { return iri; }
    public Assignment getAssignment() { return assignment; }
    public String getSubjectId() { return subjectId; }
    public String getExecutorId() { return executorId; }
    public Instant getExecutedAt() { return executedAt; }
    public Map<String, Object> getContextSnapshot() { return contextSnapshot; }
}
