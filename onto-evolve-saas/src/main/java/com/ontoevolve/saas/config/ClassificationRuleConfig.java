package com.ontoevolve.saas.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassificationRuleConfig {

    public enum RuleType { LLM, KEYWORD, HYBRID }
    public enum FallbackStrategy { AUTO_CREATE, REJECT, APPROVAL_QUEUE }

    private RuleType type;
    private int priority;
    private String promptTemplate;
    private List<String> keywords;
    private String conceptIri;
    private FallbackStrategy fallbackStrategy = FallbackStrategy.REJECT;

    public RuleType getType() { return type; }
    public void setType(RuleType type) { this.type = type; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getPromptTemplate() { return promptTemplate; }
    public void setPromptTemplate(String promptTemplate) { this.promptTemplate = promptTemplate; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public String getConceptIri() { return conceptIri; }
    public void setConceptIri(String conceptIri) { this.conceptIri = conceptIri; }

    public FallbackStrategy getFallbackStrategy() { return fallbackStrategy; }
    public void setFallbackStrategy(FallbackStrategy fallbackStrategy) { this.fallbackStrategy = fallbackStrategy; }
}
