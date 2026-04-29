package com.ontoevolve.infra.llm;

import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

/**
 * LLM 客户端抽象 — 基于 Spring AI ChatClient 的便捷封装。
 * <p>
 * 适用于简单的分类和生成任务。复杂场景（如 variator）
 * 建议直接使用 Spring AI 的 ChatClient.Builder 以获得更精细的控制。
 *
 * @see ChatClient
 */
public class LLMClient {

    private final ChatClient chatClient;
    private final ChatClient.Builder builder;

    public LLMClient(ChatClient.Builder builder, String systemPrompt) {
        this.builder = builder;
        this.chatClient = builder.defaultSystem(systemPrompt).build();
    }

    public LLMClient(ChatClient.Builder builder) {
        this(builder, "You are a helpful assistant.");
    }

    /** 发送用户消息，获取响应 */
    public String generate(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    /** 覆盖系统提示的单次调用 */
    public String generate(String systemPrompt, String userMessage) {
        ChatClient override = builder.defaultSystem(systemPrompt).build();
        return override.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    /** 批量生成多个候选 */
    public List<String> generateBatch(String prompt, int n) {
        return List.of(generate(prompt)); // 简化：实际应并行调用
    }
}
