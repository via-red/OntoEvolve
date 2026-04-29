package com.ontoevolve.core.config;

import java.util.List;
import java.util.Map;

/**
 * 框架全局配置模型 — 映射 YAML 配置。
 * <p>
 * 所有进化参数、策略选择、存储路径等全部外部化，
 * 无需重新编译即可调整系统行为。
 */
public class OntoEvolveConfig {
    private RdfConfig rdf;
    private ClassifierConfig classifier;
    private MatcherConfig matcher;
    private EvolutionConfig evolution;
    private MetaConfig meta;
    private ObservabilityConfig observability;
    private CreditConfig credit;

    // --- RDF 存储 ---
    public static class RdfConfig {
        private String storeType = "tdb2";
        private String ontologyPath = "classpath:ontology/domain.ttl";
        private String baseNamespace = "http://ontoevolve/domain#";
        private String inference = "rdfs";

        public String getStoreType() { return storeType; }
        public void setStoreType(String storeType) { this.storeType = storeType; }
        public String getOntologyPath() { return ontologyPath; }
        public void setOntologyPath(String ontologyPath) { this.ontologyPath = ontologyPath; }
        public String getBaseNamespace() { return baseNamespace; }
        public void setBaseNamespace(String baseNamespace) { this.baseNamespace = baseNamespace; }
        public String getInference() { return inference; }
        public void setInference(String inference) { this.inference = inference; }
    }

    // --- 分类器 ---
    public static class ClassifierConfig {
        private String implementation;
        private LlmConfig llm;
        private UnknownConceptConfig unknownConcept;

        public static class LlmConfig {
            private String provider = "openai";
            private String model = "gpt-4";
            private String promptTemplate = "classpath:prompts/classifier.st";
            public String getProvider() { return provider; }
            public void setProvider(String provider) { this.provider = provider; }
            public String getModel() { return model; }
            public void setModel(String model) { this.model = model; }
            public String getPromptTemplate() { return promptTemplate; }
            public void setPromptTemplate(String promptTemplate) { this.promptTemplate = promptTemplate; }
        }

        public static class UnknownConceptConfig {
            private boolean autoCreate = false;
            private String approvalQueue = "jdbc:queue:concept_approval";
            public boolean isAutoCreate() { return autoCreate; }
            public void setAutoCreate(boolean autoCreate) { this.autoCreate = autoCreate; }
            public String getApprovalQueue() { return approvalQueue; }
            public void setApprovalQueue(String approvalQueue) { this.approvalQueue = approvalQueue; }
        }

        public String getImplementation() { return implementation; }
        public void setImplementation(String implementation) { this.implementation = implementation; }
        public LlmConfig getLlm() { return llm; }
        public void setLlm(LlmConfig llm) { this.llm = llm; }
        public UnknownConceptConfig getUnknownConcept() { return unknownConcept; }
        public void setUnknownConcept(UnknownConceptConfig unknownConcept) { this.unknownConcept = unknownConcept; }
    }

    // --- 匹配器 ---
    public static class MatcherConfig {
        private String implementation = "com.ontoevolve.plugins.matcher.ParetoUCBMatcher";
        private double explorationBonus = 0.1;

        public String getImplementation() { return implementation; }
        public void setImplementation(String implementation) { this.implementation = implementation; }
        public double getExplorationBonus() { return explorationBonus; }
        public void setExplorationBonus(double explorationBonus) { this.explorationBonus = explorationBonus; }
    }

    // --- 进化引擎 ---
    public static class EvolutionConfig {
        private boolean enabled = true;
        private TriggerConfig trigger;
        private PopulationConfig population;
        private List<VariatorConfig> variators;
        private SelectorConfig selector;
        private MigrationConfig migration;

        public static class TriggerConfig {
            private FeedbackCountConfig feedbackCount;
            private ScheduleConfig schedule;
            private boolean manual = true;

            public static class FeedbackCountConfig {
                private int perNiche = 20;
                public int getPerNiche() { return perNiche; }
                public void setPerNiche(int perNiche) { this.perNiche = perNiche; }
            }

            public static class ScheduleConfig {
                private String cron = "0 0 2 * * ?";
                public String getCron() { return cron; }
                public void setCron(String cron) { this.cron = cron; }
            }

            public FeedbackCountConfig getFeedbackCount() { return feedbackCount; }
            public void setFeedbackCount(FeedbackCountConfig feedbackCount) { this.feedbackCount = feedbackCount; }
            public ScheduleConfig getSchedule() { return schedule; }
            public void setSchedule(ScheduleConfig schedule) { this.schedule = schedule; }
            public boolean isManual() { return manual; }
            public void setManual(boolean manual) { this.manual = manual; }
        }

        public static class PopulationConfig {
            private int defaultCapacity = 20;
            private Map<String, Integer> perConceptOverrides;
            public int getDefaultCapacity() { return defaultCapacity; }
            public void setDefaultCapacity(int defaultCapacity) { this.defaultCapacity = defaultCapacity; }
            public Map<String, Integer> getPerConceptOverrides() { return perConceptOverrides; }
            public void setPerConceptOverrides(Map<String, Integer> perConceptOverrides) { this.perConceptOverrides = perConceptOverrides; }
        }

        public static class SelectorConfig {
            private String implementation = "com.ontoevolve.plugins.selector.ParetoCrowdingSelector";
            public String getImplementation() { return implementation; }
            public void setImplementation(String implementation) { this.implementation = implementation; }
        }

        public static class MigrationConfig {
            private boolean enabled = false;
            private double compatibilityThreshold = 0.85;
            private String checkInterval = "P7D";
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public double getCompatibilityThreshold() { return compatibilityThreshold; }
            public void setCompatibilityThreshold(double compatibilityThreshold) { this.compatibilityThreshold = compatibilityThreshold; }
            public String getCheckInterval() { return checkInterval; }
            public void setCheckInterval(String checkInterval) { this.checkInterval = checkInterval; }
        }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public TriggerConfig getTrigger() { return trigger; }
        public void setTrigger(TriggerConfig trigger) { this.trigger = trigger; }
        public PopulationConfig getPopulation() { return population; }
        public void setPopulation(PopulationConfig population) { this.population = population; }
        public List<VariatorConfig> getVariators() { return variators; }
        public void setVariators(List<VariatorConfig> variators) { this.variators = variators; }
        public SelectorConfig getSelector() { return selector; }
        public void setSelector(SelectorConfig selector) { this.selector = selector; }
        public MigrationConfig getMigration() { return migration; }
        public void setMigration(MigrationConfig migration) { this.migration = migration; }
    }

    // --- 变异算子配置项 ---
    public static class VariatorConfig {
        private String type;        // LLM_GENERATE / CROSSOVER / PERTURB
        private int weight = 1;
        private double explorationRate = 0.15;
        private double selectionPressure = 0.7;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getWeight() { return weight; }
        public void setWeight(int weight) { this.weight = weight; }
        public double getExplorationRate() { return explorationRate; }
        public void setExplorationRate(double explorationRate) { this.explorationRate = explorationRate; }
        public double getSelectionPressure() { return selectionPressure; }
        public void setSelectionPressure(double selectionPressure) { this.selectionPressure = selectionPressure; }
    }

    // --- 元进化 ---
    public static class MetaConfig {
        private boolean enabled = false;
        private String optimizer = "com.ontoevolve.plugins.meta.BayesianMetaOptimizer";
        private List<String> parameters;
        private List<String> targetMetrics;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getOptimizer() { return optimizer; }
        public void setOptimizer(String optimizer) { this.optimizer = optimizer; }
        public List<String> getParameters() { return parameters; }
        public void setParameters(List<String> parameters) { this.parameters = parameters; }
        public List<String> getTargetMetrics() { return targetMetrics; }
        public void setTargetMetrics(List<String> targetMetrics) { this.targetMetrics = targetMetrics; }
    }

    // --- 可观测性 ---
    public static class ObservabilityConfig {
        private String evoltraceStore = "memory";
        private MetricsConfig metrics;

        public static class MetricsConfig {
            private boolean prometheusEnabled = false;
            public boolean isPrometheusEnabled() { return prometheusEnabled; }
            public void setPrometheusEnabled(boolean prometheusEnabled) { this.prometheusEnabled = prometheusEnabled; }
        }

        public String getEvoltraceStore() { return evoltraceStore; }
        public void setEvoltraceStore(String evoltraceStore) { this.evoltraceStore = evoltraceStore; }
        public MetricsConfig getMetrics() { return metrics; }
        public void setMetrics(MetricsConfig metrics) { this.metrics = metrics; }
    }

    // --- 信用分配 ---
    public static class CreditConfig {
        private String assigner = "uniform";
        private double lambda = 0.9;
        private int maxLookback = 30;

        public String getAssigner() { return assigner; }
        public void setAssigner(String assigner) { this.assigner = assigner; }
        public double getLambda() { return lambda; }
        public void setLambda(double lambda) { this.lambda = lambda; }
        public int getMaxLookback() { return maxLookback; }
        public void setMaxLookback(int maxLookback) { this.maxLookback = maxLookback; }
    }

    // --- Getters / Setters ---
    public RdfConfig getRdf() { return rdf; }
    public void setRdf(RdfConfig rdf) { this.rdf = rdf; }
    public ClassifierConfig getClassifier() { return classifier; }
    public void setClassifier(ClassifierConfig classifier) { this.classifier = classifier; }
    public MatcherConfig getMatcher() { return matcher; }
    public void setMatcher(MatcherConfig matcher) { this.matcher = matcher; }
    public EvolutionConfig getEvolution() { return evolution; }
    public void setEvolution(EvolutionConfig evolution) { this.evolution = evolution; }
    public MetaConfig getMeta() { return meta; }
    public void setMeta(MetaConfig meta) { this.meta = meta; }
    public ObservabilityConfig getObservability() { return observability; }
    public void setObservability(ObservabilityConfig observability) { this.observability = observability; }
    public CreditConfig getCredit() { return credit; }
    public void setCredit(CreditConfig credit) { this.credit = credit; }
}
