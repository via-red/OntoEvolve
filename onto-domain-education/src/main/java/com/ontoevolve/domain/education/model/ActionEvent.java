package com.ontoevolve.domain.education.model;

import com.ontoevolve.core.model.InputEvent;

import java.time.Instant;
import java.util.Map;

/**
 * 学生行为事件 — 教育领域的 InputEvent 子类。
 */
public class ActionEvent extends InputEvent {
    private final String studentId;
    private final String behaviorDescription;
    private final String location;
    private final String severity;

    public ActionEvent(String id, Instant timestamp, String behaviorDescription,
                       String studentId, String location, String severity) {
        super(id, timestamp, behaviorDescription, "education-system",
              studentId, Map.of("location", location, "severity", severity));
        this.studentId = studentId;
        this.behaviorDescription = behaviorDescription;
        this.location = location;
        this.severity = severity;
    }

    public String getStudentId() { return studentId; }
    public String getBehaviorDescription() { return behaviorDescription; }
    public String getLocation() { return location; }
    public String getSeverity() { return severity; }
}
