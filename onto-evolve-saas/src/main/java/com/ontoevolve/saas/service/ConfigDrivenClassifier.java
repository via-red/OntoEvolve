package com.ontoevolve.saas.service;

import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.InputEvent;
import com.ontoevolve.infra.llm.LLMClient;
import com.ontoevolve.saas.config.ClassificationRuleConfig;
import com.ontoevolve.saas.config.DomainDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Domain-config-driven classifier.
 * Loads classification rules from DomainCache by priority,
 * renders LLM prompt templates with event fields, matches keywords,
 * and applies fallback strategies.
 */
@Service
public class ConfigDrivenClassifier {

    private static final Logger log = LoggerFactory.getLogger(ConfigDrivenClassifier.class);

    private final DomainCache domainCache;
    private final DynamicOntologyService ontologyService;
    private final LLMClient llmClient;

    public ConfigDrivenClassifier(DomainCache domainCache,
                                   DynamicOntologyService ontologyService,
                                   LLMClient llmClient) {
        this.domainCache = domainCache;
        this.ontologyService = ontologyService;
        this.llmClient = llmClient;
    }

    /**
     * Classify an event into a Concept for the given domain.
     * Runs rules in priority order; first match wins.
     *
     * @throws ClassificationException if no rule matches and fallback cannot create
     */
    public Concept classify(String domainId, InputEvent event) {
        DomainDefinition def = domainCache.get(domainId);
        if (def == null) {
            throw new ClassificationException("Domain not found: " + domainId);
        }

        List<ClassificationRuleConfig> rules = def.getClassificationRules().stream()
                .sorted(Comparator.comparingInt(ClassificationRuleConfig::getPriority))
                .collect(Collectors.toList());

        if (rules.isEmpty()) {
            throw new ClassificationException("No classification rules configured for domain: " + domainId);
        }

        // Try each rule in priority order
        for (ClassificationRuleConfig rule : rules) {
            Concept matched = tryRule(rule, def, event);
            if (matched != null) return matched;
        }

        // No rule matched — apply fallback of the last rule
        ClassificationRuleConfig lastRule = rules.get(rules.size() - 1);
        return handleFallback(lastRule, def, event);
    }

    // ---- Rule handlers ----

    private Concept tryRule(ClassificationRuleConfig rule, DomainDefinition def, InputEvent event) {
        return switch (rule.getType()) {
            case LLM -> tryLLM(rule, def, event);
            case KEYWORD -> tryKeyword(rule, def, event);
            case HYBRID -> tryLLM(rule, def, event); // HYBRID tries LLM first
        };
    }

    private Concept tryLLM(ClassificationRuleConfig rule, DomainDefinition def, InputEvent event) {
        try {
            String prompt = renderPrompt(rule.getPromptTemplate(), def, event);
            if (prompt == null || prompt.isBlank()) return null;

            var response = llmClient.generate(prompt);
            String llmLabel = response.content().trim();

            // Match by label first, then by partial IRI
            for (Concept c : ontologyService.getConcepts(def.getId())) {
                if (c.getLabel().equalsIgnoreCase(llmLabel)
                        || c.getIri().endsWith("/" + llmLabel)
                        || c.getIri().endsWith("#" + llmLabel)) {
                    return c;
                }
            }
            // Partial match: LLM response contained in a concept label
            for (Concept c : ontologyService.getConcepts(def.getId())) {
                if (c.getLabel().toLowerCase().contains(llmLabel.toLowerCase())
                        || llmLabel.toLowerCase().contains(c.getLabel().toLowerCase())) {
                    return c;
                }
            }
        } catch (Exception e) {
            log.warn("LLM classification failed for rule priority={}: {}", rule.getPriority(), e.getMessage());
        }
        return null;
    }

    private Concept tryKeyword(ClassificationRuleConfig rule, DomainDefinition def, InputEvent event) {
        String description = event.getRawDescription();
        if (description == null || description.isBlank()) return null;
        if (rule.getKeywords() == null || rule.getKeywords().isEmpty()) return null;

        for (String keyword : rule.getKeywords()) {
            if (description.contains(keyword)) {
                if (rule.getConceptIri() != null) {
                    return ontologyService.findConcept(def.getId(), rule.getConceptIri()).orElse(null);
                }
            }
        }
        return null;
    }

    private Concept handleFallback(ClassificationRuleConfig rule, DomainDefinition def, InputEvent event) {
        ClassificationRuleConfig.FallbackStrategy strategy = rule.getFallbackStrategy();
        return switch (strategy) {
            case AUTO_CREATE -> {
                String description = event.getRawDescription();
                String label = description != null && description.length() > 20
                        ? description.substring(0, 20)
                        : (description != null ? description : "unknown");
                log.info("Auto-creating concept for label='{}' in domain '{}'", label, def.getId());
                yield ontologyService.getOrCreateConcept(def.getId(), label, null);
            }
            case REJECT ->
                throw new ClassificationException("No matching concept for event in domain: " + def.getId());
            case APPROVAL_QUEUE ->
                throw new ClassificationException("Classification requires manual approval for domain: "
                        + def.getId());
        };
    }

    // ---- Prompt rendering ----

    private String renderPrompt(String template, DomainDefinition def, InputEvent event) {
        if (template == null || template.isBlank()) return null;

        String result = template;
        result = result.replace("{{description}}",
                event.getRawDescription() != null ? event.getRawDescription() : "");
        result = result.replace("{{subjectId}}",
                event.getSubjectId() != null ? event.getSubjectId() : "");

        // Replace {{fieldName}} from event attributes
        if (event.getAttributes() != null) {
            for (Map.Entry<String, Object> attr : event.getAttributes().entrySet()) {
                if (attr.getKey().equals("domainId")) continue;
                String value = attr.getValue() != null ? attr.getValue().toString() : "";
                result = result.replace("{{" + attr.getKey() + "}}", value);
            }
        }

        // Build typeList: formatted list of all available concept labels
        String typeList = ontologyService.getConcepts(def.getId()).stream()
                .map(c -> "- " + c.getLabel() + " (" + c.getIri() + ")")
                .collect(Collectors.joining("\n"));
        result = result.replace("{{typeList}}", typeList);

        return result;
    }
}
