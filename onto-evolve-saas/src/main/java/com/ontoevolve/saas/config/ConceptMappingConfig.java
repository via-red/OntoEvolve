package com.ontoevolve.saas.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConceptMappingConfig {

    private String conceptIri;
    private List<String> decisionIris;
    private boolean autoDerive;

    public String getConceptIri() { return conceptIri; }
    public void setConceptIri(String conceptIri) { this.conceptIri = conceptIri; }

    public List<String> getDecisionIris() { return decisionIris; }
    public void setDecisionIris(List<String> decisionIris) { this.decisionIris = decisionIris; }

    public boolean isAutoDerive() { return autoDerive; }
    public void setAutoDerive(boolean autoDerive) { this.autoDerive = autoDerive; }
}
