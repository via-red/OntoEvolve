package com.ontoevolve.graphstore.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;
import java.util.List;

/**
 * Neo4j node for Assignment — connects an Intervention to a Concept
 * with evolutionary metadata (scores, lineage, status).
 */
@Node("Assignment")
public class AssignmentNode {

    @Id
    private String iri;

    private String status;

    private int trials;

    private int generation;

    private double effectiveness;

    private double cost;

    private double satisfaction;

    private double varianceEffectiveness;

    private double varianceCost;

    private double varianceSatisfaction;

    private Instant lastUpdated;

    @Relationship(type = "FOR_CONCEPT", direction = Relationship.Direction.OUTGOING)
    private ActionTypeNode concept;

    @Relationship(type = "DECIDES", direction = Relationship.Direction.OUTGOING)
    private InterventionNode intervention;

    @Relationship(type = "HAS_PARENT", direction = Relationship.Direction.OUTGOING)
    private List<AssignmentNode> parents;

    public AssignmentNode() {}

    public AssignmentNode(String iri, String status, ActionTypeNode concept,
                          InterventionNode intervention) {
        this.iri = iri;
        this.status = status;
        this.concept = concept;
        this.intervention = intervention;
        this.lastUpdated = Instant.now();
    }

    public String getIri() { return iri; }
    public void setIri(String iri) { this.iri = iri; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTrials() { return trials; }
    public void setTrials(int trials) { this.trials = trials; }
    public int getGeneration() { return generation; }
    public void setGeneration(int generation) { this.generation = generation; }
    public double getEffectiveness() { return effectiveness; }
    public void setEffectiveness(double effectiveness) { this.effectiveness = effectiveness; }
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    public double getSatisfaction() { return satisfaction; }
    public void setSatisfaction(double satisfaction) { this.satisfaction = satisfaction; }
    public double getVarianceEffectiveness() { return varianceEffectiveness; }
    public void setVarianceEffectiveness(double v) { this.varianceEffectiveness = v; }
    public double getVarianceCost() { return varianceCost; }
    public void setVarianceCost(double v) { this.varianceCost = v; }
    public double getVarianceSatisfaction() { return varianceSatisfaction; }
    public void setVarianceSatisfaction(double v) { this.varianceSatisfaction = v; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    public ActionTypeNode getConcept() { return concept; }
    public void setConcept(ActionTypeNode concept) { this.concept = concept; }
    public InterventionNode getIntervention() { return intervention; }
    public void setIntervention(InterventionNode intervention) { this.intervention = intervention; }
    public List<AssignmentNode> getParents() { return parents; }
    public void setParents(List<AssignmentNode> parents) { this.parents = parents; }
}
