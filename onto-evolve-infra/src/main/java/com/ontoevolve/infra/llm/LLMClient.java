package com.ontoevolve.infra.llm;

import com.ontoevolve.infra.metrics.MetricsCollector;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * LLM 客户端 — 基于 Spring AI ChatClient 的封装。
 * <p>
 * 支持重试、token 统计、延迟追踪、批量调用和 fallback。
 */
public class LLMClient {

    private final ChatClient chatClient;
    private final ChatClient.Builder builder;
    private final MetricsCollector metrics;

    public LLMClient(ChatClient.Builder builder, MetricsCollector metrics) {
        this.builder = builder;
        this.chatClient = builder.build();
        this.metrics = metrics;
    }

    /** 发送用户消息，返回完整 LLMResponse（含 token/延迟）。 */
    public LLMResponse generate(String userMessage) {
        return callWithMetrics(chatClient.prompt().user(userMessage));
    }

    /** 覆盖系统提示的单次调用。 */
    public LLMResponse generate(String systemPrompt, String userMessage) {
        ChatClient override = builder.defaultSystem(systemPrompt).build();
        return callWithMetrics(override.prompt().user(userMessage));
    }

    /** 批量生成多个候选（并发调用）。 */
    public List<LLMResponse> generateBatch(String prompt, int n) {
        List<CompletableFuture<LLMResponse>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> generate(prompt)));
        }
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    /** 生成并带 fallback — LLM 失败后返回 fallback 内容。 */
    public LLMResponse generateWithFallback(String prompt, String fallback) {
        try {
            return generate(prompt);
        } catch (Exception e) {
            metrics.recordLlmError();
            return new LLMResponse(fallback, 0, 0, 0, "fallback", 0);
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    private LLMResponse callWithMetrics(ChatClient.ChatClientRequestSpec spec) {
        long start = System.nanoTime();
        try {
            var response = spec.call().chatResponse();
            long latencyMs = (System.nanoTime() - start) / 1_000_000;

            String content = response.getResult().getOutput().getText();
            Usage usage = response.getMetadata().getUsage();
            String finishReason = response.getResult().getMetadata() != null
                    ? response.getResult().getMetadata().getFinishReason() : null;

            int promptTokens = usage != null ? (int) usage.getPromptTokens() : 0;
            int completionTokens = usage != null ? (int) usage.getCompletionTokens() : 0;
            int totalTokens = promptTokens + completionTokens;

            metrics.recordLlmCall(promptTokens, completionTokens, latencyMs);
            return new LLMResponse(content, promptTokens, completionTokens,
                    totalTokens, finishReason, latencyMs);
        } catch (Exception e) {
            metrics.recordLlmError();
            throw e;
        }
    }
}
