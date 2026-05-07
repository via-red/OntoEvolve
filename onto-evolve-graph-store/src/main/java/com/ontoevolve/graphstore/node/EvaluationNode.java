package com.ontoevolve.graphstore.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;

/**
 * Neo4j node for Evaluation (Feedback) — scores for an Execution.
 */
@Node("Evaluation")
public class EvaluationNode {

    @Id
    private String iri;

    private double effectiveness;

    private double cost;

    private double satisfaction;

    private Instant timestamp;

    @Relationship(type = "EVALUATES", direction = Relationship.Direction.OUTGOING)
    private ExecutionNode execution;

    public EvaluationNode() {}

    public EvaluationNode(String iri, double effectiveness, double cost,
                          double satisfaction, ExecutionNode execution) {
        this.iri = iri;
        this.effectiveness = effectiveness;
        this.cost = cost;
        this.satisfaction = satisfaction;
        this.execution = execution;
        this.timestamp = Instant.now();
    }

    public String getIri() { return iri; }
    public void setIri(String iri) { this.iri = iri; }
    public double getEffectiveness() { return effectiveness; }
    public void setEffectiveness(double effectiveness) { this.effectiveness = effectiveness; }
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    public double getSatisfaction() { return satisfaction; }
    public void setSatisfaction(double satisfaction) { this.satisfaction = satisfaction; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public ExecutionNode getExecution() { return execution; }
    public void setExecution(ExecutionNode execution) { this.execution = execution; }
}
