package com.ontoevolve.infra.llm;

/**
 * 已废弃 — 由 {@link org.springframework.ai.openai.OpenAiChatModel} + Spring AI 自动配置取代。
 * <p>
 * 如需自定义 OpenAI 客户端，请直接使用 Spring AI 的 OpenAiChatModel：
 * <pre>{@code
 * @Bean
 * public ChatClient.Builder chatClientBuilder(OpenAiChatModel model) {
 *     return ChatClient.builder(model);
 * }
 * }</pre>
 *
 * @deprecated since 0.2.0, replaced by Spring AI auto-configuration
 */
@Deprecated
public class OpenAIClient {
    private OpenAIClient() {}
}
