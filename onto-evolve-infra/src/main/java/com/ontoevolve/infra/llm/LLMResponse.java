package com.ontoevolve.infra.llm;

/**
 * 完整的 LLM 调用响应 — 包含内容和元数据。
 */
public record LLMResponse(
    String content,
    int promptTokens,
    int completionTokens,
    int totalTokens,
    String finishReason,
    long latencyMs
) {}
