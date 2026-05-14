package com.ontoevolve.saas.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DecisionTypeConfig {

    private String iri;
    private String name;
    private String description;
    private Map<String, Object> paramsSchema = Map.of();
    private List<String> stepsTemplate = List.of();

    public String getIri() { return iri; }
    public void setIri(String iri) { this.iri = iri; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getParamsSchema() { return paramsSchema; }
    public void setParamsSchema(Map<String, Object> paramsSchema) { this.paramsSchema = paramsSchema; }

    public List<String> getStepsTemplate() { return stepsTemplate; }
    public void setStepsTemplate(List<String> stepsTemplate) { this.stepsTemplate = stepsTemplate; }
}
