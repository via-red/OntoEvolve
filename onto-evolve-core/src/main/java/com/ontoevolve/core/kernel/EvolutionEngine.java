package com.ontoevolve.core.kernel;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.spi.*;
import com.ontoevolve.core.store.InMemoryPopulationStore;
import com.ontoevolve.core.validation.OntologyValidator;
import com.ontoevolve.core.config.OntoEvolveConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 进化循环引擎 — 每个 Concept 生态位的进化驱动器。
 * <p>
 * 协调变异、选择、迁移三大算子，完成一次完整的进化代。
 * 由混合触发模型驱动：反馈累积触发重排，定时调度触发完整进化。
 */
public class EvolutionEngine {
    private final Selector<Assignment> selector;
    private final List<Variator> variators;
    private final Map<String, Integer> variatorWeights;
    private final Migrator<Assignment> migrator;
    private final OntologyValidator ontologyValidator;
    private PopulationStore populationStore;
    private MetaOptimizer metaOptimizer;
    private OntoEvolveConfig config;
    private final Random random = new Random();
    private final Map<String, Instant> lastMigrationTime = new ConcurrentHashMap<>();

    public EvolutionEngine(Selector<Assignment> selector,
                           List<Variator> variators,
                           Migrator<Assignment> migrator,
                           OntologyValidator ontologyValidator) {
        this(selector, variators, migrator, ontologyValidator, null);
    }

    public EvolutionEngine(Selector<Assignment> selector,
                           List<Variator> variators,
                           Migrator<Assignment> migrator,
                           OntologyValidator ontologyValidator,
                           PopulationStore populationStore) {
        this.selector = selector;
        this.variators = variators;
        this.variatorWeights = new HashMap<>();
        this.migrator = migrator;
        this.ontologyValidator = ontologyValidator;
        this.populationStore = populationStore != null
                ? populationStore : new InMemoryPopulationStore();
    }

    public void setPopulationStore(PopulationStore populationStore) {
        this.populationStore = populationStore;
    }

    public void setMetaOptimizer(MetaOptimizer metaOptimizer) {
        this.metaOptimizer = metaOptimizer;
    }

    public void setConfig(OntoEvolveConfig config) {
        this.config = config;
        // 从配置加载变异算子权重
        if (config != null && config.getEvolution() != null
                && config.getEvolution().getVariators() != null) {
            variatorWeights.clear();
            for (var vc : config.getEvolution().getVariators()) {
                variatorWeights.put(vc.getType(), vc.getWeight());
            }
        }
    }

    /** 注册或获取一个生态位的种群 */
    public DecisionPopulation getOrCreatePopulation(Concept concept, int maxSize) {
        return populationStore.findOrCreatePopulation(concept, maxSize);
    }

    /** 记录一条反馈，累积到阈值时触发中观重排 */
    public void recordFeedback(Concept concept) {
        populationStore.incrementFeedbackCounter(concept);
        DecisionPopulation pop = populationStore.getPopulation(concept.getIri()).orElse(null);
        if (pop != null && pop.isEvolutionDue(populationStore.getFeedbackCount(concept))) {
            populationStore.resetFeedbackCounter(concept);
            runLightEvolution(concept);
        }
    }

    /** 轻量级进化 — 仅重排与选择，不含 LLM 变异 */
    private void runLightEvolution(Concept concept) {
        DecisionPopulation pop = populationStore.getPopulation(concept.getIri()).orElse(null);
        if (pop == null || pop.getActiveMembers().isEmpty()) return;

        populationStore.incrementGeneration(concept);

        // 元优化器调整选择参数（即使没有变异，也更新内部状态）
        if (metaOptimizer != null && config != null && config.getMeta().isEnabled()) {
            GlobalMetrics metrics = buildGlobalMetrics();
            metaOptimizer.optimize(metrics);
        }

        List<Assignment> candidates = new ArrayList<>(pop.getAllMembers());
        List<Assignment> selected = selector.select(candidates, pop.getMaxSize());
        applySelection(pop, selected);

        addTrace(new EvolTrace(
                "trace:" + UUID.randomUUID(),
                EvolTrace.OperationType.PERTURB,
                List.of(),
                null, null,
                "Light evolution (re-ranking) for " + concept.getLabel()
                        + " gen=" + pop.getGenerationCounter()));
    }

    /** 完整进化代 — 变异 + 选择 + 迁移 + 元优化 */
    public void runFullEvolution(Concept concept) {
        DecisionPopulation pop = populationStore.getPopulation(concept.getIri()).orElse(null);
        if (pop == null) return;

        double explorationRate = 0.15;
        // 元优化器调整探索率
        if (metaOptimizer != null && config != null && config.getMeta().isEnabled()) {
            GlobalMetrics metrics = buildGlobalMetrics();
            Map<String, Double> metaParams = metaOptimizer.optimize(metrics);
            explorationRate = metaParams.getOrDefault("explorationRate", explorationRate);
        }

        populationStore.incrementGeneration(concept);
        List<Assignment> current = pop.getActiveMembers();

        // 1. 变异：按配置权重随机选一个变异算子
        if (!variators.isEmpty() && !current.isEmpty()) {
            Variator selectedVariator = selectVariatorByWeight();
            VariationContext ctx = new VariationContext(
                    concept, current, Map.of(), explorationRate);
            List<Assignment> newborns = selectedVariator.generate(ctx);

            // 本体验证 + 记录轨迹
            for (Assignment newborn : newborns) {
                if (ontologyValidator.validate(newborn)) {
                    pop.addMember(newborn);
                    addTrace(new EvolTrace(
                            "trace:" + UUID.randomUUID(),
                            EvolTrace.OperationType.valueOf(selectedVariator.type()),
                            newborn.getParents() != null ? newborn.getParents() : List.of(),
                            newborn, newborn.getDecision(),
                            "Generation " + pop.getGenerationCounter()));
                }
            }
        }

        // 2. 选择
        List<Assignment> allCandidates = new ArrayList<>(pop.getAllMembers());
        List<Assignment> selected = selector.select(allCandidates, pop.getMaxSize());
        applySelection(pop, selected);

        // 3. 迁移（仅当启用）
        if (migrator != null && config != null
                && config.getEvolution().getMigration().isEnabled()) {
            checkAndMigrate(concept, pop);
        }
    }

    private void applySelection(DecisionPopulation pop, List<Assignment> selected) {
        Set<Assignment> selectedSet = new HashSet<>(selected);
        for (Assignment member : pop.getAllMembers()) {
            if (!selectedSet.contains(member)) {
                if (member.getStatus() == Assignment.Status.PROBATION) {
                    member.setStatus(Assignment.Status.DEPRECATED);
                } else {
                    member.setStatus(Assignment.Status.PROBATION);
                }
            }
        }
        for (Assignment member : selected) {
            if (member.getStatus() != Assignment.Status.ELITE) {
                member.setStatus(Assignment.Status.ACTIVE);
            }
        }
        pop.replaceMembers(selected);
    }

    private Variator selectVariatorByWeight() {
        if (variators.isEmpty()) throw new IllegalStateException("No variators registered");
        if (variators.size() == 1) return variators.get(0);

        // 权重驱动选择：按配置权重累计，随机落点
        int totalWeight = variators.stream()
                .mapToInt(v -> variatorWeights.getOrDefault(v.type(), 1))
                .sum();
        if (totalWeight <= 0) return variators.get(random.nextInt(variators.size()));

        int point = random.nextInt(totalWeight);
        int cumulative = 0;
        for (Variator v : variators) {
            cumulative += variatorWeights.getOrDefault(v.type(), 1);
            if (point < cumulative) return v;
        }
        return variators.get(variators.size() - 1);
    }

    private GlobalMetrics buildGlobalMetrics() {
        double avgHypervolume = populationStore.getAllPopulations().values().stream()
                .filter(p -> !p.getActiveMembers().isEmpty())
                .mapToDouble(this::computeHypervolume)
                .average().orElse(0.0);
        return new GlobalMetrics(
                avgHypervolume, 0.0, 0.0, 0.0,
                populationStore.getAllPopulations().size(), 0, Map.of());
    }

    private double computeHypervolume(DecisionPopulation pop) {
        List<Assignment> members = pop.getActiveMembers();
        if (members.size() < 2) return 0.0;
        double[] scores = members.get(0).getScoreVector();
        int dim = scores.length;
        if (dim == 0) return 0.0;
        double[] refPoint = new double[dim];
        // 以最差值为参考点
        for (int d = 0; d < dim; d++) {
            final int dd = d;
            refPoint[d] = members.stream()
                    .mapToDouble(a -> a.getScoreVector()[dd])
                    .min().orElse(0) - 0.1;
        }
        double hv = 0.0;
        for (Assignment a : members) {
            double vol = 1.0;
            for (int d = 0; d < dim; d++) {
                vol *= Math.max(0, a.getScoreVector()[d] - refPoint[d]);
            }
            hv += vol;
        }
        return hv;
    }

    private void checkAndMigrate(Concept concept, DecisionPopulation pop) {
        // Time window check: skip if migration happened within checkInterval
        if (config != null && config.getEvolution().getMigration().getCheckInterval() != null) {
            Instant last = lastMigrationTime.get(concept.getIri());
            Duration interval = Duration.parse(config.getEvolution().getMigration().getCheckInterval());
            if (last != null && Duration.between(last, Instant.now()).compareTo(interval) < 0) {
                return; // Not yet time for next migration
            }
        }
        lastMigrationTime.put(concept.getIri(), Instant.now());

        List<Assignment> elites = pop.getActiveMembers().stream()
                .filter(a -> a.getStatus() == Assignment.Status.ELITE)
                .collect(Collectors.toList());
        if (elites.isEmpty()) return;

        for (Map.Entry<String, DecisionPopulation> entry : populationStore.getAllPopulations().entrySet()) {
            if (entry.getKey().equals(concept.getIri())) continue;
            Concept targetConcept = entry.getValue().getConcept();
            List<Assignment> migrants = migrator.proposeMigrations(
                    concept, targetConcept, elites);
            for (Assignment migrant : migrants) {
                if (ontologyValidator.validate(migrant)) {
                    entry.getValue().addMember(migrant);
                }
            }
        }
    }

    public void addTrace(EvolTrace trace) {
        populationStore.addTrace(trace);
    }

    public List<EvolTrace> getTraces() { return populationStore.getTraces(); }
    public Map<String, DecisionPopulation> getPopulations() { return populationStore.getAllPopulations(); }
}
