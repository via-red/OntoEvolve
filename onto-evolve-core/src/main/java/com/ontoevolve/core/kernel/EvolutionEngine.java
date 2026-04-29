package com.ontoevolve.core.kernel;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.spi.Migrator;
import com.ontoevolve.core.spi.Selector;
import com.ontoevolve.core.spi.VariationContext;
import com.ontoevolve.core.spi.Variator;
import com.ontoevolve.core.validation.OntologyValidator;

import java.util.*;
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
    private final Migrator<Assignment> migrator;
    private final OntologyValidator ontologyValidator;
    private final Map<String, DecisionPopulation> populations;
    private final Map<String, Integer> feedbackCounters;
    private final List<EvolTrace> traces;
    private final Random random;

    public EvolutionEngine(Selector<Assignment> selector,
                           List<Variator> variators,
                           Migrator<Assignment> migrator,
                           OntologyValidator ontologyValidator) {
        this.selector = selector;
        this.variators = variators;
        this.migrator = migrator;
        this.ontologyValidator = ontologyValidator;
        this.populations = new HashMap<>();
        this.feedbackCounters = new HashMap<>();
        this.traces = new ArrayList<>();
        this.random = new Random();
    }

    /** 注册或获取一个生态位的种群 */
    public DecisionPopulation getOrCreatePopulation(Concept concept, int maxSize) {
        return populations.computeIfAbsent(concept.getIri(),
                k -> new DecisionPopulation(concept, maxSize));
    }

    /** 记录一条反馈，累积到阈值时触发中观重排 */
    public void recordFeedback(Concept concept) {
        feedbackCounters.merge(concept.getIri(), 1, Integer::sum);
        DecisionPopulation pop = populations.get(concept.getIri());
        if (pop != null && pop.isEvolutionDue(feedbackCounters.get(concept.getIri()))) {
            feedbackCounters.put(concept.getIri(), 0);
            runLightEvolution(concept);
        }
    }

    /** 轻量级进化 — 仅重排与选择，不含 LLM 变异 */
    private void runLightEvolution(Concept concept) {
        DecisionPopulation pop = populations.get(concept.getIri());
        if (pop == null || pop.getActiveMembers().isEmpty()) return;

        List<Assignment> candidates = new ArrayList<>(pop.getAllMembers());
        List<Assignment> selected = selector.select(candidates, pop.getMaxSize());
        applySelection(pop, selected);
    }

    /** 完整进化代 — 变异 + 选择 + 迁移 */
    public void runFullEvolution(Concept concept) {
        DecisionPopulation pop = populations.get(concept.getIri());
        if (pop == null) return;

        pop.incrementGeneration();
        List<Assignment> current = pop.getActiveMembers();

        // 1. 变异：按权重随机选一个变异算子
        if (!variators.isEmpty() && !current.isEmpty()) {
            Variator selectedVariator = selectVariatorByWeight();
            VariationContext ctx = new VariationContext(
                    concept, current, Map.of(), 0.15);
            List<Assignment> newborns = selectedVariator.generate(ctx);

            // 本体验证 + 记录轨迹
            for (Assignment newborn : newborns) {
                if (ontologyValidator.validate(newborn)) {
                    pop.addMember(newborn);
                }
            }
        }

        // 2. 选择
        List<Assignment> allCandidates = new ArrayList<>(pop.getAllMembers());
        List<Assignment> selected = selector.select(allCandidates, pop.getMaxSize());
        applySelection(pop, selected);

        // 3. 迁移（异步风格检查）
        if (migrator != null) {
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
        // 均等权重选择（实际应由配置驱动）
        return variators.get(random.nextInt(variators.size()));
    }

    private void checkAndMigrate(Concept concept, DecisionPopulation pop) {
        List<Assignment> elites = pop.getActiveMembers().stream()
                .filter(a -> a.getStatus() == Assignment.Status.ELITE)
                .collect(Collectors.toList());
        if (elites.isEmpty()) return;

        for (Map.Entry<String, DecisionPopulation> entry : populations.entrySet()) {
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
        traces.add(trace);
    }

    public List<EvolTrace> getTraces() { return Collections.unmodifiableList(traces); }
    public Map<String, DecisionPopulation> getPopulations() { return populations; }
}
