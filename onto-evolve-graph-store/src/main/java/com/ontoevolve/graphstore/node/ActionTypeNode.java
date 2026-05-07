package com.ontoevolve.graphstore.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j node for ontology concept / action type.
 * Self-referencing hierarchy via SUBSUMES relationship.
 */
@Node("ActionType")
public class ActionTypeNode {

    @Id
    private String iri;

    private String label;

    private String category;

    private String comment;

    @Relationship(type = "SUBSUMES", direction = Relationship.Direction.OUTGOING)
    private ActionTypeNode parent;

    @Relationship(type = "SUBSUMES", direction = Relationship.Direction.INCOMING)
    private List<ActionTypeNode> children;

    public ActionTypeNode() {}

    public ActionTypeNode(String iri, String label, String category, String comment) {
        this.iri = iri;
        this.label = label;
        this.category = category;
        this.comment = comment;
        this.children = new ArrayList<>();
    }

    public String getIri() { return iri; }
    public void setIri(String iri) { this.iri = iri; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public ActionTypeNode getParent() { return parent; }
    public void setParent(ActionTypeNode parent) { this.parent = parent; }
    public List<ActionTypeNode> getChildren() { return children; }
    public void setChildren(List<ActionTypeNode> children) { this.children = children; }
}
