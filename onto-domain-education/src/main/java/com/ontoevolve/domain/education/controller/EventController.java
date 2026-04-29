package com.ontoevolve.domain.education.controller;

import com.ontoevolve.core.kernel.DecisionPopulation;
import com.ontoevolve.core.kernel.EvolTrace;
import com.ontoevolve.core.kernel.EvolutionEngine;
import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Execution;
import com.ontoevolve.domain.education.model.ActionEvent;
import com.ontoevolve.domain.education.model.ActionType;
import com.ontoevolve.domain.education.model.Intervention;
import com.ontoevolve.domain.education.service.InterventionService;
import com.ontoevolve.infra.metrics.MetricsCollector;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * 教育领域 REST API。
 * <p>
 * 提供事件处理、反馈提交、种群监控等端点。
 */
@RestController
@RequestMapping("/api/education")
public class EventController {

    private final InterventionService interventionService;
    private final MetricsCollector metrics;

    public EventController(InterventionService interventionService,
                           MetricsCollector metrics) {
        this.interventionService = interventionService;
        this.metrics = metrics;
    }

    /**
     * 处理一条行为事件，返回推荐的干预方案。
     */
    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> handleEvent(@RequestBody Map<String, String> body) {
        ActionEvent event = new ActionEvent(
                "event:" + UUID.randomUUID(),
                Instant.now(),
                body.get("description"),
                body.get("studentId"),
                body.getOrDefault("location", ""),
                body.getOrDefault("severity", "medium")
        );

        Intervention suggestion = interventionService.processEvent(event);

        Map<String, Object> response = new HashMap<>();
        response.put("eventId", event.getId());
        response.put("suggestion", suggestion != null ? suggestion.getName() : "无匹配方案");
        response.put("studentId", event.getStudentId());

        return ResponseEntity.ok(response);
    }

    /**
     * 提交干预效果评价。
     */
    @PostMapping("/evaluation")
    public ResponseEntity<String> submitEvaluation(@RequestBody Map<String, String> body) {
        // 简化：实际应查询关联的 Execution
        interventionService.submitEvaluation(
                null,
                Double.parseDouble(body.getOrDefault("effectiveness", "0.5")),
                Double.parseDouble(body.getOrDefault("cost", "0.3")),
                Double.parseDouble(body.getOrDefault("satisfaction", "0.8"))
        );

        return ResponseEntity.ok("评价已提交");
    }

    /**
     * 查看指定生态位的方案种群。
     */
    @GetMapping("/populations/{conceptIri}")
    public ResponseEntity<Map<String, Object>> getPopulation(@PathVariable String conceptIri) {
        EvolutionEngine engine = interventionService.getEvolutionEngine();
        DecisionPopulation pop = engine.getPopulations().get(conceptIri);

        if (pop == null) {
            return ResponseEntity.notFound().build();
        }

        List<Map<String, Object>> members = pop.getActiveMembers().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", a.getDecision().getName());
            m.put("score", Arrays.toString(a.getScoreVector()));
            m.put("trials", a.getTrials());
            m.put("status", a.getStatus());
            m.put("generation", a.getGeneration());
            return m;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("concept", conceptIri);
        response.put("generation", pop.getGenerationCounter());
        response.put("size", pop.size());
        response.put("members", members);

        return ResponseEntity.ok(response);
    }

    /**
     * 手动触发完整进化代。
     */
    @PostMapping("/evolve")
    public ResponseEntity<String> triggerEvolution(@RequestParam String conceptIri) {
        interventionService.evolveNiche(conceptIri);
        return ResponseEntity.ok("进化触发完成");
    }

    /**
     * 查看全局系统指标。
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        EvolutionEngine engine = interventionService.getEvolutionEngine();
        var globalMetrics = metrics.snapshot(engine.getPopulations());

        Map<String, Object> response = new HashMap<>();
        response.put("totalFeedbacks", globalMetrics.getTotalFeedback());
        response.put("llmCalls", globalMetrics.getLlmCallCost());
        response.put("averageHypervolume", globalMetrics.getAverageHypervolume());
        response.put("nicheDiversity", globalMetrics.getNicheDiversityIndex());
        response.put("totalPopulations", globalMetrics.getTotalPopulations());

        return ResponseEntity.ok(response);
    }

    /**
     * 查看进化谱系。
     */
    @GetMapping("/traces")
    public ResponseEntity<List<Map<String, Object>>> getTraces() {
        EvolutionEngine engine = interventionService.getEvolutionEngine();
        List<Map<String, Object>> traces = engine.getTraces().stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("type", t.getOperationType());
            m.put("timestamp", t.getTimestamp().toString());
            m.put("decision", t.getProducedDecision().getName());
            m.put("context", t.getContextDescription());
            return m;
        }).toList();

        return ResponseEntity.ok(traces);
    }
}
