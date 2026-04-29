package com.ontoevolve.infra.metrics;

import com.ontoevolve.core.kernel.DecisionPopulation;
import com.ontoevolve.core.kernel.EvolTrace;
import com.ontoevolve.core.spi.GlobalMetrics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 度量收集器 — 收集系统运行指标，供元优化器和监控使用。
 * <p>
 * 记录种群大小、反馈数、LLM 调用次数、进化代际等关键指标。
 * 可与 Micrometer 集成暴露 Prometheus 端点。
 */
public class MetricsCollector {
    private final Map<String, AtomicInteger> populationSizes = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> generationCounters = new ConcurrentHashMap<>();
    private final AtomicLong totalFeedbacks = new AtomicLong(0);
    private final AtomicInteger llmCalls = new AtomicInteger(0);
    private final AtomicInteger evolutionRuns = new AtomicInteger(0);
    private final List<Double> hypervolumeHistory = Collections.synchronizedList(new ArrayList<>());

    public void recordPopulationSize(String conceptIri, int size) {
        populationSizes.computeIfAbsent(conceptIri, k -> new AtomicInteger()).set(size);
    }

    public void recordGeneration(String conceptIri, int generation) {
        generationCounters.computeIfAbsent(conceptIri, k -> new AtomicInteger()).set(generation);
    }

    public void recordFeedback() {
        totalFeedbacks.incrementAndGet();
    }

    public void recordLlmCall() {
        llmCalls.incrementAndGet();
    }

    public void recordEvolutionRun() {
        evolutionRuns.incrementAndGet();
    }

    public void recordHypervolume(double hv) {
        hypervolumeHistory.add(hv);
        if (hypervolumeHistory.size() > 1000) {
            hypervolumeHistory.remove(0);
        }
    }

    public int getPopulationSize(String conceptIri) {
        return populationSizes.getOrDefault(conceptIri, new AtomicInteger(0)).get();
    }

    public int getGeneration(String conceptIri) {
        return generationCounters.getOrDefault(conceptIri, new AtomicInteger(0)).get();
    }

    public long getTotalFeedbacks() { return totalFeedbacks.get(); }
    public int getLlmCalls() { return llmCalls.get(); }
    public int getEvolutionRuns() { return evolutionRuns.get(); }

    public double getAverageHypervolume() {
        return hypervolumeHistory.isEmpty() ? 0.0 :
                hypervolumeHistory.stream().mapToDouble(d -> d).average().orElse(0.0);
    }

    public double getNicheDiversityIndex(Map<String, DecisionPopulation> populations) {
        if (populations.isEmpty()) return 0.0;
        long totalActive = populations.values().stream()
                .mapToLong(p -> p.getActiveMembers().size())
                .sum();
        if (totalActive == 0) return 0.0;

        double shannon = 0.0;
        for (DecisionPopulation pop : populations.values()) {
            double p = (double) pop.getActiveMembers().size() / totalActive;
            if (p > 0) shannon -= p * Math.log(p);
        }
        return shannon / Math.log(populations.size());
    }

    public GlobalMetrics snapshot(Map<String, DecisionPopulation> populations) {
        Map<String, Double> extra = new HashMap<>();
        extra.put("evolutionRuns", (double) getEvolutionRuns());
        extra.put("llmCalls", (double) getLlmCalls());

        return new GlobalMetrics(
                getAverageHypervolume(),
                getNicheDiversityIndex(populations),
                0.0, // deprecationRate
                getLlmCalls(),
                populations.size(),
                getTotalFeedbacks(),
                extra
        );
    }
}
