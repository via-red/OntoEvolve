package com.ontoevolve.saas.service;

import com.ontoevolve.core.model.Concept;
import com.ontoevolve.saas.config.ConceptConfig;
import com.ontoevolve.saas.config.DomainDefinition;
import com.ontoevolve.saas.dto.ConceptTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Builds runtime Concept objects from domain ontology configuration.
 * Caches built concept trees per domain, invalidated on config changes.
 */
@Service
public class DynamicOntologyService {

    private static final Logger log = LoggerFactory.getLogger(DynamicOntologyService.class);

    private final DomainCache domainCache;
    private final DomainConfigService configService;
    private final Map<String, List<Concept>> conceptCache = new ConcurrentHashMap<>();

    public DynamicOntologyService(DomainCache domainCache, DomainConfigService configService) {
        this.domainCache = domainCache;
        this.configService = configService;
    }

    /**
     * Get all concepts for a domain, built from config and cached.
     */
    public List<Concept> getConcepts(String domainId) {
        return conceptCache.computeIfAbsent(domainId, id -> {
            DomainDefinition def = domainCache.get(id);
            if (def == null) return List.of();
            return buildConcepts(def);
        });
    }

    /**
     * Find a concept by its IRI within a domain.
     */
    public Optional<Concept> findConcept(String domainId, String iri) {
        return getConcepts(domainId).stream()
                .filter(c -> c.getIri().equals(iri))
                .findFirst();
    }

    /**
     * Find direct children of a concept.
     */
    public List<Concept> findChildren(String domainId, Concept parent) {
        return getConcepts(domainId).stream()
                .filter(c -> c.hasParent() && c.getParentConcept().getIri().equals(parent.getIri()))
                .collect(Collectors.toList());
    }

    /**
     * Get the full concept tree as nested DTOs for API response.
     */
    public List<ConceptTreeNode> getConceptTree(String domainId) {
        List<Concept> all = getConcepts(domainId);
        return all.stream()
                .filter(c -> !c.hasParent())
                .map(root -> buildTreeNode(root, all))
                .collect(Collectors.toList());
    }

    /**
     * Get a concept with its full parent chain (for hierarchy walking).
     */
    public List<Concept> getConceptWithAncestors(String domainId, String iri) {
        List<Concept> chain = new ArrayList<>();
        Optional<Concept> current = findConcept(domainId, iri);
        while (current.isPresent()) {
            chain.add(current.get());
            current = current.get().hasParent()
                    ? Optional.of(current.get().getParentConcept())
                    : Optional.empty();
        }
        return chain;
    }

    /**
     * Create a new concept dynamically (for LLM-suggested new concepts).
     * Persists to config and invalidates cache.
     */
    public Concept getOrCreateConcept(String domainId, String label, String parentIri) {
        DomainDefinition def = domainCache.get(domainId);
        if (def == null) throw new IllegalArgumentException("Domain not found: " + domainId);

        String namespace = def.getDomain().getNamespace();
        String iri = namespace + label.replaceAll("\\s+", "");

        Optional<Concept> existing = findConcept(domainId, iri);
        if (existing.isPresent()) return existing.get();

        ConceptConfig cc = new ConceptConfig();
        cc.setIri(iri);
        cc.setLabel(label);
        cc.setParentIri(parentIri);
        cc.setProperties(Map.of("color", "#cccccc", "icon", "auto"));

        def.getConcepts().add(cc);
        try {
            configService.saveDomain(def);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist new concept: " + iri, e);
        }

        invalidateCache(domainId);

        Concept parent = parentIri != null
                ? findConcept(domainId, parentIri).orElse(null) : null;
        Concept concept = new Concept(iri, label, parent);
        cc.getProperties().forEach(concept.getProperties()::put);
        return concept;
    }

    /**
     * Invalidate the concept cache for a domain (called when config changes).
     */
    public void invalidateCache(String domainId) {
        conceptCache.remove(domainId);
    }

    // ---- Private helpers ----

    private List<Concept> buildConcepts(DomainDefinition def) {
        List<ConceptConfig> configs = def.getConcepts().stream()
                .sorted(Comparator.comparingInt(ConceptConfig::getSortOrder))
                .toList();

        Map<String, Concept> conceptMap = new LinkedHashMap<>();

        for (ConceptConfig cc : configs) {
            Concept parent = cc.getParentIri() != null
                    ? conceptMap.get(cc.getParentIri()) : null;
            Concept concept = new Concept(cc.getIri(), cc.getLabel(), parent);
            if (cc.getProperties() != null) {
                cc.getProperties().forEach(concept.getProperties()::put);
            }
            conceptMap.put(cc.getIri(), concept);
        }

        return List.copyOf(conceptMap.values());
    }

    private ConceptTreeNode buildTreeNode(Concept concept, List<Concept> all) {
        ConceptTreeNode node = new ConceptTreeNode();
        node.setIri(concept.getIri());
        node.setLabel(concept.getLabel());
        node.setParentIri(concept.hasParent() ? concept.getParentConcept().getIri() : null);
        node.setProperties(concept.getProperties());
        node.setChildren(all.stream()
                .filter(c -> c.hasParent() && c.getParentConcept().getIri().equals(concept.getIri()))
                .map(c -> buildTreeNode(c, all))
                .collect(Collectors.toList()));
        return node;
    }
}
