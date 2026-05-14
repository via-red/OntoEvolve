package com.ontoevolve.saas.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Aggregate DTO representing a complete domain definition loaded from JSON files.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DomainDefinition {

    private DomainConfig domain;
    private List<ConceptConfig> concepts;
    private List<DecisionTypeConfig> decisionTypes;
    private List<ConceptMappingConfig> conceptMappings;
    private List<FeedbackDimensionConfig> feedbackDimensions;
    private List<ClassificationRuleConfig> classificationRules;
    private List<FieldDefinitionConfig> fieldDefinitions;

    public String getId() { return domain != null ? domain.getId() : null; }

    public DomainConfig getDomain() { return domain; }
    public void setDomain(DomainConfig domain) { this.domain = domain; }

    public List<ConceptConfig> getConcepts() { return concepts; }
    public void setConcepts(List<ConceptConfig> concepts) { this.concepts = concepts; }

    public List<DecisionTypeConfig> getDecisionTypes() { return decisionTypes; }
    public void setDecisionTypes(List<DecisionTypeConfig> decisionTypes) { this.decisionTypes = decisionTypes; }

    public List<ConceptMappingConfig> getConceptMappings() { return conceptMappings; }
    public void setConceptMappings(List<ConceptMappingConfig> conceptMappings) { this.conceptMappings = conceptMappings; }

    public List<FeedbackDimensionConfig> getFeedbackDimensions() { return feedbackDimensions; }
    public void setFeedbackDimensions(List<FeedbackDimensionConfig> feedbackDimensions) { this.feedbackDimensions = feedbackDimensions; }

    public List<ClassificationRuleConfig> getClassificationRules() { return classificationRules; }
    public void setClassificationRules(List<ClassificationRuleConfig> classificationRules) { this.classificationRules = classificationRules; }

    public List<FieldDefinitionConfig> getFieldDefinitions() { return fieldDefinitions; }
    public void setFieldDefinitions(List<FieldDefinitionConfig> fieldDefinitions) { this.fieldDefinitions = fieldDefinitions; }
}
