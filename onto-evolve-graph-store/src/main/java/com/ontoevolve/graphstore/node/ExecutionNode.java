package com.ontoevolve.graphstore.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;

/**
 * Neo4j node for Execution — records when an Assignment was applied to a subject.
 */
@Node("Execution")
public class ExecutionNode {

    @Id
    private String iri;

    private String subjectId;

    private String executorId;

    private Instant executedAt;

    @Relationship(type = "EXECUTES", direction = Relationship.Direction.OUTGOING)
    private AssignmentNode assignment;

    public ExecutionNode() {}

    public ExecutionNode(String iri, String subjectId, String executorId,
                         AssignmentNode assignment) {
        this.iri = iri;
        this.subjectId = subjectId;
        this.executorId = executorId;
        this.assignment = assignment;
        this.executedAt = Instant.now();
    }

    public String getIri() { return iri; }
    public void setIri(String iri) { this.iri = iri; }
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    public String getExecutorId() { return executorId; }
    public void setExecutorId(String executorId) { this.executorId = executorId; }
    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }
    public AssignmentNode getAssignment() { return assignment; }
    public void setAssignment(AssignmentNode assignment) { this.assignment = assignment; }
}
