package com.ontoevolve.graphstore.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;
import java.util.List;

/**
 * Neo4j node for EvolTrace — records evolutionary provenance.
 */
@Node("EvolTrace")
public class EvolTraceNode {

    @Id
    private String id;

    private String operationType;

    private String contextDescription;

    private Instant timestamp;

    @Relationship(type = "PRODUCED", direction = Relationship.Direction.OUTGOING)
    private AssignmentNode producedAssignment;

    @Relationship(type = "PRODUCED_DECISION", direction = Relationship.Direction.OUTGOING)
    private InterventionNode producedDecision;

    @Relationship(type = "DERIVED_FROM", direction = Relationship.Direction.OUTGOING)
    private List<AssignmentNode> parentAssignments;

    public EvolTraceNode() {}

    public EvolTraceNode(String id, String operationType, String contextDescription,
                         AssignmentNode producedAssignment,
                         InterventionNode producedDecision,
                         List<AssignmentNode> parentAssignments) {
        this.id = id;
        this.operationType = operationType;
        this.contextDescription = contextDescription;
        this.producedAssignment = producedAssignment;
        this.producedDecision = producedDecision;
        this.parentAssignments = parentAssignments;
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getContextDescription() { return contextDescription; }
    public void setContextDescription(String contextDescription) { this.contextDescription = contextDescription; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public AssignmentNode getProducedAssignment() { return producedAssignment; }
    public void setProducedAssignment(AssignmentNode producedAssignment) { this.producedAssignment = producedAssignment; }
    public InterventionNode getProducedDecision() { return producedDecision; }
    public void setProducedDecision(InterventionNode producedDecision) { this.producedDecision = producedDecision; }
    public List<AssignmentNode> getParentAssignments() { return parentAssignments; }
    public void setParentAssignments(List<AssignmentNode> parentAssignments) { this.parentAssignments = parentAssignments; }
}
