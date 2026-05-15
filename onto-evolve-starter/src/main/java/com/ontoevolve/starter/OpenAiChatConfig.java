package com.ontoevolve.starter;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * 默认的 OpenAI / DeepSeek ChatClient.Builder 配置。
 * <p>
 * 当领域模块没有提供自己的 ChatClient.Builder 时自动生效。
 * 如需切换 LLM 供应商，领域模块声明自己的 ChatClient.Builder {@code @Bean} 即可覆盖。
 */
@Configuration
@ConditionalOnClass(OpenAiApi.class)
public class OpenAiChatConfig {

    @Bean
    @ConditionalOnMissingBean
    public RetryTemplate retryTemplate() {
        RetryTemplate template = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        template.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(1000);
        backOff.setMultiplier(2.0);
        template.setBackOffPolicy(backOff);

        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAiApi openAiApi(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.base-url:https://api.deepseek.com}") String baseUrl) {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAiChatModel openAiChatModel(
            OpenAiApi openAiApi,
            @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}") String model,
            RetryTemplate retryTemplate,
            ObservationRegistry observationRegistry) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatClient.Builder chatClientBuilder(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }
}
