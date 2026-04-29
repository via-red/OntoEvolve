package com.ontoevolve.starter;

import com.ontoevolve.core.config.OntoEvolveConfig;
import com.ontoevolve.core.kernel.EvolutionEngine;
import com.ontoevolve.core.spi.*;
import com.ontoevolve.core.validation.OntologyValidator;
import com.ontoevolve.infra.llm.LLMClient;
import com.ontoevolve.infra.metrics.MetricsCollector;
import com.ontoevolve.plugins.matcher.ParetoUCBMatcher;
import com.ontoevolve.plugins.migrator.SemanticMigrator;
import com.ontoevolve.plugins.selector.ParetoCrowdingSelector;
import com.ontoevolve.plugins.variator.CrossoverVariator;
import com.ontoevolve.plugins.variator.LLMGenerateVariator;
import com.ontoevolve.plugins.variator.PerturbVariator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
@EnableConfigurationProperties(OntoEvolveConfig.class)
public class OntoEvolveAutoConfiguration {

    // ==================== Spring AI ChatClient ====================

    /**
     * ChatClient.Builder 由 Spring AI 自动配置提供（基于 spring.ai.* 配置）。
     * 可在此处覆盖默认 system prompt 或自定义构建逻辑。
     */
    @Bean
    @ConditionalOnMissingBean
    public LLMClient llmClient(ChatClient.Builder builder) {
        return new LLMClient(builder, "你是一个专业的语义分类助手。");
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
    public List<Variator<?, ?>> variators(ChatClient.Builder builder,
                                          OntoEvolveConfig config) {
        var llm = new LLMGenerateVariator(builder);
        var crossover = new CrossoverVariator(builder);
        var perturb = new PerturbVariator();
        return List.of(llm, crossover, perturb);
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
            OntologyValidator ontologyValidator) {
        return new EvolutionEngine(
                (Selector) selector,
                (List) variators,
                (Migrator) migrator,
                ontologyValidator);
    }

    // ==================== 基础设施 ====================

    @Bean
    @ConditionalOnMissingBean
    public MetricsCollector metricsCollector() {
        return new MetricsCollector();
    }

    @Bean
    @ConditionalOnMissingBean
    public OntologyValidator ontologyValidator() {
        return new OntologyValidator() {
            @Override
            public boolean validate(com.ontoevolve.core.model.Assignment assignment) {
                return true; // 默认允许所有，由领域项目覆盖
            }
        };
    }
}
