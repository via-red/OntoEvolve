package com.ontoevolve.starter;

import com.ontoevolve.core.config.OntoEvolveConfig;
import com.ontoevolve.core.kernel.EvolutionEngine;
import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Execution;
import com.ontoevolve.core.model.Feedback;
import com.ontoevolve.core.spi.*;
import com.ontoevolve.core.store.InMemoryPopulationStore;
import com.ontoevolve.core.validation.OntologyValidator;
import com.ontoevolve.infra.llm.LLMClient;
import com.ontoevolve.infra.metrics.MetricsCollector;
import com.ontoevolve.infra.store.OntologyStore;
import com.ontoevolve.infra.store.Tdb2OntologyStore;
import com.ontoevolve.plugins.credit.UniformCreditAssigner;
import com.ontoevolve.plugins.matcher.ParetoUCBMatcher;
import com.ontoevolve.plugins.meta.BayesianMetaOptimizer;
import com.ontoevolve.plugins.migrator.SemanticMigrator;
import com.ontoevolve.plugins.selector.ParetoCrowdingSelector;
import com.ontoevolve.plugins.variator.CrossoverVariator;
import com.ontoevolve.plugins.variator.LLMGenerateVariator;
import com.ontoevolve.plugins.variator.PerturbVariator;
import com.ontoevolve.infra.llm.PromptTemplateService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.util.List;

/**
 * OntoEvolve Spring Boot AutoConfiguration。
 * <p>
 * 自动装配核心 Bean，所有组件可通过配置替换。
 * 领域项目只需声明自己的 Bean 即可覆盖默认实现。
 * <p>
 * AI 交互基于 Spring AI ChatClient — 支持 OpenAI / Claude / Ollama 等后端，
 * 通过 spring.ai.openai.api-key 等标准配置驱动。
 */
@Configuration
@EnableRetry
public class OntoEvolveAutoConfiguration {

    // ==================== 配置 ====================

    @Bean
    @ConfigurationProperties(prefix = "onto")
    public OntoEvolveConfig ontoEvolveConfig() {
        return new OntoEvolveConfig();
    }

    // ==================== Spring AI ChatClient ====================

    /**
     * ChatClient.Builder 由领域模块的 @Bean 提供（如 OpenAiChatConfig），
     * 以便 Starter 保持 LLM 供应商无关。
     */
    @Bean
    @ConditionalOnMissingBean
    public LLMClient llmClient(ChatClient.Builder builder, MetricsCollector metrics) {
        return new LLMClient(builder, metrics);
    }

    @Bean
    @ConditionalOnMissingBean
    public PromptTemplateService promptTemplateService(org.springframework.core.io.ResourceLoader resourceLoader) {
        return new PromptTemplateService(resourceLoader);
    }

    // ==================== SPI 默认实现 ====================

    @Bean
    @ConditionalOnMissingBean(Selector.class)
    @ConditionalOnProperty(prefix = "onto.evolution.selector", name = "implementation",
            havingValue = "com.ontoevolve.plugins.selector.ParetoCrowdingSelector",
            matchIfMissing = true)
    public Selector<?> paretoCrowdingSelector() {
        return new ParetoCrowdingSelector();
    }

    @Bean
    @ConditionalOnMissingBean(Migrator.class)
    @ConditionalOnProperty(prefix = "onto.evolution.migration", name = "enabled",
            havingValue = "true", matchIfMissing = false)
    public Migrator<?> semanticMigrator(OntoEvolveConfig config) {
        return new SemanticMigrator(
                config.getEvolution().getMigration().getCompatibilityThreshold());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "onto.matcher", name = "implementation",
            havingValue = "com.ontoevolve.plugins.matcher.ParetoUCBMatcher",
            matchIfMissing = true)
    public Matcher<?, ?, ?> paretoUCBMatcher(OntoEvolveConfig config) {
        return new ParetoUCBMatcher(config.getMatcher().getExplorationBonus());
    }

    // ==================== 变异算子 ====================

    @Bean
    @ConditionalOnProperty(prefix = "onto.evolution.variators", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public List<Variator<?, ?>> variators(LLMClient llmClient,
                                          PromptTemplateService promptService) {
        var llm = new LLMGenerateVariator(llmClient, promptService);
        var crossover = new CrossoverVariator(llmClient, promptService);
        var perturb = new PerturbVariator();
        return List.of(llm, crossover, perturb);
    }

    // ==================== 信用分配 ====================

    @Bean
    @ConditionalOnMissingBean(CreditAssigner.class)
    @ConditionalOnProperty(prefix = "onto.credit", name = "assigner",
            havingValue = "com.ontoevolve.plugins.credit.UniformCreditAssigner",
            matchIfMissing = true)
    public CreditAssigner uniformCreditAssigner(OntoEvolveConfig config) {
        return new UniformCreditAssigner(
                config.getCredit().getLambda(),
                config.getCredit().getMaxLookback());
    }

    // ==================== 环境/模拟器 ====================

    @Bean
    @ConditionalOnMissingBean(Environment.class)
    @ConditionalOnProperty(prefix = "onto.environment", name = "simulated",
            havingValue = "true")
    public Environment simulatedEnvironment(OntoEvolveConfig config) {
        var env = config.getEnvironment();
        return new com.ontoevolve.plugins.env.SimulatedEvaluationEnvironment(
                env.getNoiseSigma(), env.getBias());
    }

    @Bean
    @ConditionalOnMissingBean(Environment.class)
    public Environment defaultEnvironment() {
        return new Environment() {
            @Override
            public Feedback evaluate(Assignment assignment, Execution execution) {
                return null;
            }

            @Override
            public boolean isAutomated() {
                return false;
            }
        };
    }

    // ==================== 元优化器 ====================

    @Bean
    @ConditionalOnMissingBean(MetaOptimizer.class)
    @ConditionalOnProperty(prefix = "onto.meta", name = "enabled",
            havingValue = "true", matchIfMissing = false)
    public MetaOptimizer bayesianMetaOptimizer() {
        return new BayesianMetaOptimizer();
    }

    // ==================== 进化引擎 ====================

    @Bean
    @ConditionalOnMissingBean(EvolutionEngine.class)
    @ConditionalOnProperty(prefix = "onto.evolution", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public EvolutionEngine evolutionEngine(
            List<Variator<?, ?>> variators,
            Selector<?> selector,
            Migrator<?> migrator,
            OntologyValidator ontologyValidator,
            OntoEvolveConfig config,
            ObjectProvider<MetaOptimizer> metaOptimizerProvider,
            ObjectProvider<PopulationStore> populationStoreProvider) {
        PopulationStore store = populationStoreProvider.getIfAvailable();
        EvolutionEngine engine = new EvolutionEngine(
                (Selector) selector,
                (List) variators,
                (Migrator) migrator,
                ontologyValidator,
                store);
        engine.setConfig(config);
        MetaOptimizer metaOptimizer = metaOptimizerProvider.getIfAvailable();
        if (metaOptimizer != null) {
            engine.setMetaOptimizer(metaOptimizer);
        }
        return engine;
    }

    // ==================== 基础设施 ====================

    @Bean
    @ConditionalOnMissingBean(OntologyStore.class)
    @ConditionalOnProperty(prefix = "onto.rdf", name = "store-type",
            havingValue = "tdb2", matchIfMissing = true)
    public OntologyStore ontologyStore(OntoEvolveConfig config) {
        var rdf = config.getRdf();
        String storePath = System.getProperty("java.io.tmpdir") + "/ontoevolve-tdb2";
        Tdb2OntologyStore store = new Tdb2OntologyStore(storePath);
        store.initialize(rdf.getOntologyPath(), rdf.getBaseNamespace(), rdf.getInference());
        return store;
    }

    @Bean
    @ConditionalOnMissingBean
    public MetricsCollector metricsCollector() {
        return new MetricsCollector();
    }

    @Bean
    @ConditionalOnMissingBean
    public OntologyValidator ontologyValidator(ObjectProvider<OntologyStore> ontologyStoreProvider) {
        OntologyStore store = ontologyStoreProvider.getIfAvailable();
        if (store instanceof Tdb2OntologyStore tdb2Store) {
            return new com.ontoevolve.infra.validation.JenaOntologyValidator(tdb2Store);
        }
        return assignment -> true;
    }

    @Bean
    @ConditionalOnMissingBean(PopulationStore.class)
    public PopulationStore inMemoryPopulationStore() {
        return new InMemoryPopulationStore();
    }
}
