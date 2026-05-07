package com.ontoevolve.graphstore.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;

/**
 * Neo4j node for ActionEvent — records behavior events linked to ontology types.
 */
@Node("ActionEvent")
public class ActionEventNode {

    @Id
    private String eventId;

    private String studentId;

    private String description;

    private String location;

    private String severity;

    private Instant timestamp;

    private String matchedIntervention;

    @Relationship(type = "CLASSIFIED_AS", direction = Relationship.Direction.OUTGOING)
    private ActionTypeNode classifiedConcept;

    public ActionEventNode() {}

    public ActionEventNode(String eventId, String studentId, String description,
                           String location, String severity, ActionTypeNode classifiedConcept) {
        this.eventId = eventId;
        this.studentId = studentId;
        this.description = description;
        this.location = location;
        this.severity = severity;
        this.classifiedConcept = classifiedConcept;
        this.timestamp = Instant.now();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getMatchedIntervention() { return matchedIntervention; }
    public void setMatchedIntervention(String matchedIntervention) { this.matchedIntervention = matchedIntervention; }
    public ActionTypeNode getClassifiedConcept() { return classifiedConcept; }
    public void setClassifiedConcept(ActionTypeNode classifiedConcept) { this.classifiedConcept = classifiedConcept; }
}
