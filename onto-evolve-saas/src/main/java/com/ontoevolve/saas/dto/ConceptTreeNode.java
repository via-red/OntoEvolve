package com.ontoevolve.saas.dto;

import java.util.List;
import java.util.Map;

public class ConceptTreeNode {
    private String iri;
    private String label;
    private String parentIri;
    private Map<String, Object> properties;
    private List<ConceptTreeNode> children;

    public String getIri() { return iri; }
    public void setIri(String iri) { this.iri = iri; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getParentIri() { return parentIri; }
    public void setParentIri(String parentIri) { this.parentIri = parentIri; }

    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }

    public List<ConceptTreeNode> getChildren() { return children; }
    public void setChildren(List<ConceptTreeNode> children) { this.children = children; }
}
