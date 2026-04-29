package com.ontoevolve.plugins.variator;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.spi.Variator;
import com.ontoevolve.core.spi.VariationContext;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * LLM 生成变异算子 — 利用 Spring AI ChatClient 生成全新方案。
 * <p>
 * Prompt 会注入当前生态位的成功方案特征摘要，
 * 使变异具有偏向性而非完全随机。
 * 支持通过配置切换不同的 AI 模型（OpenAI / Claude / Ollama 等）。
 */
public class LLMGenerateVariator implements Variator<Decision, Assignment> {

    private final ChatClient chatClient;

    public LLMGenerateVariator(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一个决策方案设计师，擅长根据已有成功方案的特征生成创新的备选方案。")
                .build();
    }

    @Override
    public List<Assignment> generate(VariationContext ctx) {
        List<Assignment> newborns = new ArrayList<>();
        String nicheSummary = summarizeNiche(ctx);
        String prompt = buildUserPrompt(ctx.getConcept().getLabel(), nicheSummary);

        String llmOutput = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        Decision decision = parseDecision(llmOutput, ctx);
        if (decision != null) {
            Assignment assignment = new Assignment(
                    "gen:" + UUID.randomUUID(),
                    decision,
                    ctx.getConcept(),
                    3 // 默认三维反馈
            );
            newborns.add(assignment);
        }
        return newborns;
    }

    @Override
    public String type() { return "LLM_GENERATE"; }

    private String summarizeNiche(VariationContext ctx) {
        return ctx.getPopulation().stream()
                .filter(a -> a.getStatus() == Assignment.Status.ACTIVE
                        || a.getStatus() == Assignment.Status.ELITE)
                .limit(5)
                .map(a -> String.format("[%s] score=%s trials=%d",
                        a.getDecision().getName(),
                        java.util.Arrays.toString(a.getScoreVector()),
                        a.getTrials()))
                .collect(Collectors.joining("\n"));
    }

    private String buildUserPrompt(String conceptLabel, String nicheSummary) {
        return String.format("""
                当前生态位: %s。

                已有成功方案概览:
                %s

                请生成一个新的干预方案，要求:
                1. 名称 — 简练概括方案核心
                2. 描述 — 一句话说明适用场景和预期效果
                3. 执行步骤 — 3-5 步，清晰可操作

                要求借鉴已有方案的优势，但提供不同的解决思路。
                """, conceptLabel, nicheSummary);
    }

    private Decision parseDecision(String llmOutput, VariationContext ctx) {
        String iri = "decision:" + UUID.randomUUID();
        String name = extractName(llmOutput);
        String desc = extractDescription(llmOutput);
        List<String> steps = extractSteps(llmOutput);

        Decision d = new Decision(iri, name != null ? name : "LLM方案", desc, steps);
        if (!ctx.getPopulation().isEmpty()) {
            d.addParent(ctx.getPopulation().get(0).getDecision());
        }
        return d;
    }

    private String extractName(String text) {
        if (text == null) return null;
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("名称") || trimmed.startsWith("Name")
                    || trimmed.startsWith("1.") || trimmed.startsWith("1、")) {
                String name = trimmed.replaceAll("^[\\d.、]*\\s*", "")
                        .replaceAll("^名称[:：]?\\s*", "")
                        .replaceAll("^Name[:：]?\\s*", "");
                if (!name.isEmpty()) return name;
            }
        }
        return null;
    }

    private String extractDescription(String text) {
        return text != null && text.length() > 200 ? text.substring(0, 200) : text;
    }

    private List<String> extractSteps(String text) {
        if (text == null) return List.of();
        List<String> steps = new ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.matches("^\\d+[.、]\\s.*") && steps.size() < 5) {
                steps.add(trimmed.replaceAll("^\\d+[.、]\\s*", ""));
            }
        }
        return steps.isEmpty() ? List.of("执行方案") : steps;
    }
}
