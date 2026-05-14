package com.ontoevolve.saas.dto;

import java.util.List;
import java.util.Map;

public class EventResult {
    private String recordId;
    private String conceptIri;
    private String conceptLabel;
    private String decisionIri;
    private String decisionName;
    private List<String> steps;
    private String assignmentIri;
    private double[] scoreVector;
    private Map<String, Object> attributes;

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getConceptIri() { return conceptIri; }
    public void setConceptIri(String conceptIri) { this.conceptIri = conceptIri; }

    public String getConceptLabel() { return conceptLabel; }
    public void setConceptLabel(String conceptLabel) { this.conceptLabel = conceptLabel; }

    public String getDecisionIri() { return decisionIri; }
    public void setDecisionIri(String decisionIri) { this.decisionIri = decisionIri; }

    public String getDecisionName() { return decisionName; }
    public void setDecisionName(String decisionName) { this.decisionName = decisionName; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }

    public String getAssignmentIri() { return assignmentIri; }
    public void setAssignmentIri(String assignmentIri) { this.assignmentIri = assignmentIri; }

    public double[] getScoreVector() { return scoreVector; }
    public void setScoreVector(double[] scoreVector) { this.scoreVector = scoreVector; }

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}
