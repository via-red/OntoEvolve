package com.ontoevolve.core.model;

import java.time.Instant;
import java.util.Map;

/**
 * 域外输入事件 — 整个决策循环的起点。
 * <p>
 * 携带原始信息，尚未被分类到任何 Concept 生态位。
 */
public class InputEvent {
    private final String id;
    private final Instant timestamp;
    private final String rawDescription;
    private final String sourceSystem;
    private final String subjectId;
    private final Map<String, Object> attributes;

    public InputEvent(String id, Instant timestamp, String rawDescription,
                      String sourceSystem, String subjectId,
                      Map<String, Object> attributes) {
        this.id = id;
        this.timestamp = timestamp;
        this.rawDescription = rawDescription;
        this.sourceSystem = sourceSystem;
        this.subjectId = subjectId;
        this.attributes = attributes;
    }

    public String getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getRawDescription() { return rawDescription; }
    public String getSourceSystem() { return sourceSystem; }
    public String getSubjectId() { return subjectId; }
    public Map<String, Object> getAttributes() { return attributes; }
}
