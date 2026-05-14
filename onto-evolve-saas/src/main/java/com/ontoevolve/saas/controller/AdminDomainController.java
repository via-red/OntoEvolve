package com.ontoevolve.saas.controller;

import com.ontoevolve.saas.config.*;
import com.ontoevolve.saas.dto.ConceptTreeNode;
import com.ontoevolve.saas.service.DomainConfigService;
import com.ontoevolve.saas.service.DomainStoreManagerService;
import com.ontoevolve.saas.service.DynamicOntologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin API for domain configuration CRUD, publishing, and ontology management.
 */
@RestController
@RequestMapping("/api/admin/domains")
public class AdminDomainController {

    private static final Logger log = LoggerFactory.getLogger(AdminDomainController.class);

    private final DomainConfigService configService;
    private final DynamicOntologyService ontologyService;
    private final DomainStoreManagerService storeManagerService;

    public AdminDomainController(DomainConfigService configService,
                                  DynamicOntologyService ontologyService,
                                  DomainStoreManagerService storeManagerService) {
        this.configService = configService;
        this.ontologyService = ontologyService;
        this.storeManagerService = storeManagerService;
    }

    // ---- Domain CRUD ----

    @GetMapping
    public List<Map<String, Object>> listDomains() {
        return configService.getAllDomains().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createDomain(@RequestBody Map<String, String> body) {
        String id = body.get("id");
        String name = body.get("name");
        if (id == null || id.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "id is required"));
        }
        try {
            DomainDefinition def = configService.createDomain(id, name);
            return ResponseEntity.status(HttpStatus.CREATED).body(toSummary(def));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DomainDefinition> getDomain(@PathVariable String id) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(def);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDomain(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();

        DomainConfig config = def.getDomain();
        if (body.containsKey("name")) config.setName((String) body.get("name"));
        if (body.containsKey("description")) config.setDescription((String) body.get("description"));
        if (body.containsKey("namespace")) config.setNamespace((String) body.get("namespace"));

        try {
            configService.saveDomain(def);
            ontologyService.invalidateCache(id);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok(toSummary(def));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDomain(@PathVariable String id) {
        try {
            configService.deleteDomain(id);
            ontologyService.invalidateCache(id);
            storeManagerService.unregister(id);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishDomain(@PathVariable String id) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();

        def.getDomain().setStatus(DomainStatus.PUBLISHED);
        try {
            configService.saveDomain(def);
            ontologyService.invalidateCache(id);
            storeManagerService.register(def);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok(Map.of("status", "published"));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<Map<String, Object>> unpublishDomain(@PathVariable String id) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();

        def.getDomain().setStatus(DomainStatus.ARCHIVED);
        try {
            configService.saveDomain(def);
            ontologyService.invalidateCache(id);
            storeManagerService.unregister(id);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok(Map.of("status", "unpublished"));
    }

    // ---- Ontology (concepts) ----

    @GetMapping("/{id}/concepts")
    public ResponseEntity<List<ConceptTreeNode>> getConcepts(@PathVariable String id) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ontologyService.getConceptTree(id));
    }

    @PostMapping("/{id}/concepts")
    public ResponseEntity<Map<String, Object>> addConcept(
            @PathVariable String id, @RequestBody ConceptConfig conceptConfig) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();

        def.getConcepts().add(conceptConfig);
        try {
            configService.saveDomain(def);
            ontologyService.invalidateCache(id);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("iri", conceptConfig.getIri()));
    }

    @PutMapping("/{id}/concepts/{cid}")
    public ResponseEntity<Void> updateConcept(
            @PathVariable String id, @PathVariable String cid,
            @RequestBody ConceptConfig update) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();

        Optional<ConceptConfig> existing = def.getConcepts().stream()
                .filter(c -> c.getIri().equals(cid)).findFirst();
        if (existing.isEmpty()) return ResponseEntity.notFound().build();

        ConceptConfig cc = existing.get();
        if (update.getLabel() != null) cc.setLabel(update.getLabel());
        if (update.getParentIri() != null) cc.setParentIri(update.getParentIri());
        if (update.getProperties() != null) cc.setProperties(update.getProperties());
        if (update.getConstraints() != null) cc.setConstraints(update.getConstraints());

        try {
            configService.saveDomain(def);
            ontologyService.invalidateCache(id);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/concepts/{cid}")
    public ResponseEntity<Void> deleteConcept(
            @PathVariable String id, @PathVariable String cid) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();

        def.getConcepts().removeIf(c -> c.getIri().equals(cid));
        try {
            configService.saveDomain(def);
            ontologyService.invalidateCache(id);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.noContent().build();
    }

    // ---- Decision Types ----

    @GetMapping("/{id}/decision-types")
    public ResponseEntity<List<DecisionTypeConfig>> getDecisionTypes(@PathVariable String id) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(def.getDecisionTypes());
    }

    @PostMapping("/{id}/decision-types")
    public ResponseEntity<Map<String, Object>> addDecisionType(
            @PathVariable String id, @RequestBody DecisionTypeConfig dtc) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();

        def.getDecisionTypes().add(dtc);
        try {
            configService.saveDomain(def);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("iri", dtc.getIri()));
    }

    @DeleteMapping("/{id}/decision-types/{dtid}")
    public ResponseEntity<Void> deleteDecisionType(
            @PathVariable String id, @PathVariable String dtid) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();

        def.getDecisionTypes().removeIf(d -> d.getIri().equals(dtid));
        try {
            configService.saveDomain(def);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.noContent().build();
    }

    // ---- Feedback Dimensions ----

    @GetMapping("/{id}/feedback-dimensions")
    public ResponseEntity<List<FeedbackDimensionConfig>> getFeedbackDimensions(@PathVariable String id) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(def.getFeedbackDimensions());
    }

    @PostMapping("/{id}/feedback-dimensions")
    public ResponseEntity<Map<String, Object>> addFeedbackDimension(
            @PathVariable String id, @RequestBody FeedbackDimensionConfig fdc) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();

        def.getFeedbackDimensions().add(fdc);
        try {
            configService.saveDomain(def);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("name", fdc.getName()));
    }

    // ---- Classification Rules ----

    @GetMapping("/{id}/classification-rules")
    public ResponseEntity<List<ClassificationRuleConfig>> getClassificationRules(@PathVariable String id) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(def.getClassificationRules());
    }

    @PostMapping("/{id}/classification-rules")
    public ResponseEntity<Map<String, Object>> addClassificationRule(
            @PathVariable String id, @RequestBody ClassificationRuleConfig rule) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();

        def.getClassificationRules().add(rule);
        try {
            configService.saveDomain(def);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("priority", rule.getPriority()));
    }

    // ---- Concept Mappings ----

    @GetMapping("/{id}/mappings")
    public ResponseEntity<List<ConceptMappingConfig>> getMappings(@PathVariable String id) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(def.getConceptMappings());
    }

    @PostMapping("/{id}/mappings")
    public ResponseEntity<Map<String, Object>> addMapping(
            @PathVariable String id, @RequestBody ConceptMappingConfig mapping) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();

        def.getConceptMappings().add(mapping);
        try {
            configService.saveDomain(def);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("conceptIri", mapping.getConceptIri()));
    }

    @DeleteMapping("/{id}/mappings/{mid}")
    public ResponseEntity<Void> deleteMapping(
            @PathVariable String id, @PathVariable String mid) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();

        def.getConceptMappings().removeIf(m -> m.getConceptIri().equals(mid));
        try {
            configService.saveDomain(def);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.noContent().build();
    }

    // ---- Field Definitions ----

    @GetMapping("/{id}/field-definitions")
    public ResponseEntity<List<FieldDefinitionConfig>> getFieldDefinitions(@PathVariable String id) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(def.getFieldDefinitions());
    }

    @PostMapping("/{id}/field-definitions")
    public ResponseEntity<Map<String, Object>> addFieldDefinition(
            @PathVariable String id, @RequestBody FieldDefinitionConfig fdc) {
        DomainDefinition def = configService.getDomain(id);
        if (def == null) return ResponseEntity.notFound().build();

        def.getFieldDefinitions().add(fdc);
        try {
            configService.saveDomain(def);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("fieldName", fdc.getFieldName()));
    }

    // ---- Helpers ----

    private Map<String, Object> toSummary(DomainDefinition def) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", def.getId());
        summary.put("name", def.getDomain().getName());
        summary.put("description", def.getDomain().getDescription());
        summary.put("status", def.getDomain().getStatus().name());
        summary.put("namespace", def.getDomain().getNamespace());
        summary.put("conceptCount", def.getConcepts() != null ? def.getConcepts().size() : 0);
        summary.put("decisionTypeCount", def.getDecisionTypes() != null ? def.getDecisionTypes().size() : 0);
        summary.put("published", def.getDomain().getStatus() == DomainStatus.PUBLISHED);
        return summary;
    }
}
