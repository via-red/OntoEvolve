package com.ontoevolve.infra.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "spring.ai.openai", name = "api-key")
public class OpenAiChatConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatConfig.class);

    @Bean
    @ConditionalOnMissingBean
    public OpenAiApi openAiApi(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl) {
        return OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAiChatModel openAiChatModel(OpenAiApi api,
                                           @Value("${spring.ai.openai.chat.options.model:deepseek-chat}") String model,
                                           @Value("${spring.ai.openai.chat.options.temperature:0.7}") Double temperature) {
        log.info("Creating OpenAiChatModel with model={}, temperature={}", model, temperature);
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatClient.Builder chatClientBuilder(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }
}
