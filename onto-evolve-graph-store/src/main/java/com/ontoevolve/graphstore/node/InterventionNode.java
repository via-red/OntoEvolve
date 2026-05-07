package com.ontoevolve.graphstore.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;
import java.util.List;

/**
 * Neo4j node for Decision/Intervention.
 * Combines both core Decision and education Intervention fields.
 */
@Node("Intervention")
public class InterventionNode {

    @Id
    private String iri;

    private String name;

    private String description;

    private String interventionType;

    private boolean requiresParentApproval;

    private Instant createdAt;

    private List<String> steps;

    @Relationship(type = "HAS_PARENT", direction = Relationship.Direction.OUTGOING)
    private List<InterventionNode> parents;

    public InterventionNode() {}

    public InterventionNode(String iri, String name, String description,
                            String interventionType, boolean requiresParentApproval) {
        this.iri = iri;
        this.name = name;
        this.description = description;
        this.interventionType = interventionType;
        this.requiresParentApproval = requiresParentApproval;
        this.createdAt = Instant.now();
    }

    public String getIri() { return iri; }
    public void setIri(String iri) { this.iri = iri; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInterventionType() { return interventionType; }
    public void setInterventionType(String interventionType) { this.interventionType = interventionType; }
    public boolean isRequiresParentApproval() { return requiresParentApproval; }
    public void setRequiresParentApproval(boolean requiresParentApproval) { this.requiresParentApproval = requiresParentApproval; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }
    public List<InterventionNode> getParents() { return parents; }
    public void setParents(List<InterventionNode> parents) { this.parents = parents; }
}
