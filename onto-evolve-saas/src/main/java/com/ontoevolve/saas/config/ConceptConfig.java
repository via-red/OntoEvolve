package com.ontoevolve.saas.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConceptConfig {

    private String iri;
    private String label;
    private String parentIri;
    private Map<String, Object> properties = Map.of();
    private Map<String, Object> constraints = Map.of();
    private int sortOrder;

    public String getIri() { return iri; }
    public void setIri(String iri) { this.iri = iri; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getParentIri() { return parentIri; }
    public void setParentIri(String parentIri) { this.parentIri = parentIri; }

    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }

    public Map<String, Object> getConstraints() { return constraints; }
    public void setConstraints(Map<String, Object> constraints) { this.constraints = constraints; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
