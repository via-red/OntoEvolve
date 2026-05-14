package com.ontoevolve.saas.controller;

import com.ontoevolve.core.model.InputEvent;
import com.ontoevolve.saas.dto.EventResult;
import com.ontoevolve.saas.dto.GraphDataDTO;
import com.ontoevolve.saas.dto.PopulationDTO;
import com.ontoevolve.saas.service.DomainOrchestrationService;
import com.ontoevolve.saas.service.DynamicOntologyService;
import com.ontoevolve.saas.store.DomainStore;
import com.ontoevolve.saas.store.DomainStoreManager;
import com.ontoevolve.saas.dto.ConceptTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * User-facing domain API — event records, feedback, ontology, populations, evolution.
 * All endpoints are scoped under /{domainId}.
 */
@RestController
public class DomainController {

    private static final Logger log = LoggerFactory.getLogger(DomainController.class);

    private final DomainOrchestrationService orchestrationService;
    private final DynamicOntologyService ontologyService;
    private final DomainStoreManager storeManager;

    public DomainController(DomainOrchestrationService orchestrationService,
                             DynamicOntologyService ontologyService,
                             DomainStoreManager storeManager) {
        this.orchestrationService = orchestrationService;
        this.ontologyService = ontologyService;
        this.storeManager = storeManager;
    }

    // ---- Records ----

    @PostMapping("/{domainId}/records")
    public ResponseEntity<?> submitRecord(
            @PathVariable String domainId,
            @RequestBody Map<String, Object> body) {
        try {
            EventResult result = orchestrationService.processEvent(domainId, body);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing event for domain {}: {}", domainId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{domainId}/records")
    public ResponseEntity<?> listRecords(
            @PathVariable String domainId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Map<String, Object> filters) {
        try {
            DomainStore store = storeManager.getStore(domainId);
            List<InputEvent> records = store.getRecords(filters, page, size);
            long total = store.countRecords(filters);
            return ResponseEntity.ok(Map.of(
                    "records", records,
                    "page", page,
                    "size", size,
                    "total", total));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{domainId}/records/{recordId}")
    public ResponseEntity<?> getRecord(
            @PathVariable String domainId,
            @PathVariable String recordId) {
        try {
            DomainStore store = storeManager.getStore(domainId);
            return store.getRecord(recordId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---- Feedback ----

    /**
     * Submit feedback for a record.
     * Request body: { assignmentIri, scores: { dimensionName: value, ... }, attributes: { ... } }
     */
    @PostMapping("/{domainId}/records/{recordId}/feedback")
    public ResponseEntity<?> submitFeedback(
            @PathVariable String domainId,
            @PathVariable String recordId,
            @RequestBody Map<String, Object> body) {
        try {
            String assignmentIri = (String) body.get("assignmentIri");
            if (assignmentIri == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "assignmentIri is required"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Double> scores = body.get("scores") instanceof Map
                    ? (Map<String, Double>) body.get("scores")
                    : Map.of();

            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = body.get("attributes") instanceof Map
                    ? (Map<String, Object>) body.get("attributes")
                    : Map.of();

            orchestrationService.submitFeedback(domainId, recordId, assignmentIri, scores, attributes);
            return ResponseEntity.ok(Map.of("status", "feedback recorded"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error submitting feedback for domain {}: {}", domainId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ---- Ontology ----

    @GetMapping("/{domainId}/ontology")
    public ResponseEntity<?> getOntology(@PathVariable String domainId) {
        try {
            List<ConceptTreeNode> tree = ontologyService.getConceptTree(domainId);
            return ResponseEntity.ok(Map.of("concepts", tree));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---- Populations ----

    @GetMapping("/{domainId}/populations")
    public ResponseEntity<?> getPopulations(@PathVariable String domainId) {
        try {
            List<PopulationDTO> populations = orchestrationService.getPopulations(domainId);
            return ResponseEntity.ok(Map.of("populations", populations));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---- Evolution ----

    /**
     * Trigger evolution for a concept niche.
     * Request body: { conceptIri: "..." }
     */
    @PostMapping("/{domainId}/evolve")
    public ResponseEntity<?> triggerEvolution(
            @PathVariable String domainId,
            @RequestBody Map<String, String> body) {
        try {
            String conceptIri = body.get("conceptIri");
            if (conceptIri == null || conceptIri.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "conceptIri is required"));
            }
            orchestrationService.triggerEvolution(domainId, conceptIri);
            return ResponseEntity.ok(Map.of("status", "evolution triggered"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---- Graph ----

    @GetMapping("/{domainId}/graph")
    public ResponseEntity<?> getGraph(
            @PathVariable String domainId,
            @RequestParam(required = false) String conceptIri) {
        try {
            GraphDataDTO graph = orchestrationService.getGraphData(domainId, conceptIri);
            return ResponseEntity.ok(graph);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---- Metrics ----

    @GetMapping("/{domainId}/metrics")
    public ResponseEntity<?> getMetrics(@PathVariable String domainId) {
        try {
            Map<String, Object> metrics = orchestrationService.getMetrics(domainId);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
