package com.ontoevolve.plugins.variator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.spi.Variator;
import com.ontoevolve.core.spi.VariationContext;
import com.ontoevolve.infra.llm.LLMClient;
import com.ontoevolve.infra.llm.LLMResponse;
import com.ontoevolve.infra.llm.PromptTemplateService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LLM 生成变异算子 — 利用 LLM 生成全新方案。
 * <p>
 * 使用 PromptTemplateService 加载外部 prompt 模板，
 * 优先以 JSON 格式解析 LLM 输出，失败时回退 regex 解析。
 */
public class LLMGenerateVariator implements Variator<Decision, Assignment> {

    private final LLMClient llmClient;
    private final PromptTemplateService promptService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LLMGenerateVariator(LLMClient llmClient, PromptTemplateService promptService) {
        this.llmClient = llmClient;
        this.promptService = promptService;
    }

    @Override
    public List<Assignment> generate(VariationContext ctx) {
        List<Assignment> newborns = new ArrayList<>();
        String nicheSummary = summarizeNiche(ctx);

        String prompt = promptService.render("variator_generate", Map.of(
                "conceptLabel", ctx.getConcept().getLabel(),
                "nicheSummary", nicheSummary
        ));

        LLMResponse response = llmClient.generate(prompt);
        if (response.content() == null || response.content().isBlank()) return newborns;

        Decision decision = parseDecision(response.content(), ctx);
        if (decision != null) {
            Assignment assignment = new Assignment(
                    "gen:" + UUID.randomUUID(),
                    decision,
                    ctx.getConcept(),
                    3
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
                        Arrays.toString(a.getScoreVector()),
                        a.getTrials()))
                .collect(Collectors.joining("\n"));
    }

    private Decision parseDecision(String llmOutput, VariationContext ctx) {
        String iri = "decision:" + UUID.randomUUID();

        // 优先 JSON 解析
        try {
            // 提取 JSON 块（可能被 markdown ``` 包围）
            String json = llmOutput;
            if (json.contains("```")) {
                int start = json.indexOf('{');
                int end = json.lastIndexOf('}');
                if (start >= 0 && end > start) json = json.substring(start, end + 1);
            }
            Map<?, ?> parsed = objectMapper.readValue(json, Map.class);
            String name = Objects.toString(parsed.get("name"), null);
            String desc = Objects.toString(parsed.get("description"), null);
            List<?> stepsRaw = (List<?>) parsed.get("steps");
            List<String> steps = stepsRaw != null
                    ? stepsRaw.stream().map(Object::toString).toList()
                    : List.of("执行方案");

            Decision d = new Decision(iri, name != null ? name : "LLM方案", desc, steps);
            if (!ctx.getPopulation().isEmpty()) {
                d.addParent(ctx.getPopulation().get(0).getDecision());
            }
            return d;
        } catch (Exception ignored) {
            // JSON 解析失败，回退 regex
        }

        // 回退 regex 解析
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
