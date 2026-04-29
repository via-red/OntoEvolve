package com.ontoevolve.plugins.variator;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.spi.Variator;
import com.ontoevolve.core.spi.VariationContext;
import org.springframework.ai.chat.client.ChatClient;

import java.util.*;

/**
 * 重组变异算子 — 从种群中选择两个高分亲本，由 LLM 融合生成新方案。
 * <p>
 * 模拟进化计算中的交叉操作 (Crossover)。
 * 亲本按 Pareto 前沿分层选择，高前沿者优先。
 */
public class CrossoverVariator implements Variator<Decision, Assignment> {

    private final ChatClient chatClient;
    private final Random random;

    public CrossoverVariator(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一个决策方案设计师，擅长融合两个成功方案的优势生成更优的方案。")
                .build();
        this.random = new Random();
    }

    @Override
    public List<Assignment> generate(VariationContext ctx) {
        List<Assignment> population = ctx.getPopulation();
        if (population.size() < 2) return List.of();

        // 选择两个高分亲本（按 score 向量欧几里得范数排序）
        List<Assignment> sorted = population.stream()
                .sorted(Comparator.comparingDouble(
                        a -> -euclideanNorm(a.getScoreVector())))
                .toList();

        Assignment parentA = sorted.get(0);
        Assignment parentB = sorted.get(random.nextInt(Math.min(3, sorted.size())));

        String userPrompt = String.format("""
                请将以下两个方案融合为一个新的、更优的方案，继承双方的优势:

                【亲本 A】
                %s
                效果评分: %s | 尝试次数: %d

                【亲本 B】
                %s
                效果评分: %s | 尝试次数: %d

                所在生态位: %s

                要求:
                1. 融合两个方案的核心优势，避免各自的短板
                2. 给出名称、描述和 3-5 步执行步骤
                """,
                describeDecision(parentA), Arrays.toString(parentA.getScoreVector()), parentA.getTrials(),
                describeDecision(parentB), Arrays.toString(parentB.getScoreVector()), parentB.getTrials(),
                ctx.getConcept().getLabel());

        String fusionResult = chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();

        Decision child = new Decision(
                "crossover:" + UUID.randomUUID(),
                extractName(fusionResult, parentA, parentB),
                fusionResult != null && fusionResult.length() > 200
                        ? fusionResult.substring(0, 200) : fusionResult,
                extractSteps(fusionResult)
        );
        child.addParent(parentA.getDecision());
        child.addParent(parentB.getDecision());

        Assignment childAssign = new Assignment(
                "asgn:" + UUID.randomUUID(),
                child,
                ctx.getConcept(),
                parentA.getScoreVector().length
        );
        childAssign.setParents(List.of(parentA, parentB));
        childAssign.setGeneration(parentA.getGeneration() + 1);

        return List.of(childAssign);
    }

    @Override
    public String type() { return "CROSSOVER"; }

    private String describeDecision(Assignment a) {
        Decision d = a.getDecision();
        return String.format("[%s]\n描述: %s\n步骤: %s",
                d.getName(), d.getDescription(),
                String.join("; ", d.getSteps()));
    }

    private String extractName(String fusion, Assignment a, Assignment b) {
        if (fusion == null) return a.getDecision().getName() + " × " + b.getDecision().getName();
        for (String line : fusion.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("名称") || trimmed.startsWith("Name")) {
                String name = trimmed.replaceAll("^名称[:：]?\\s*", "")
                        .replaceAll("^Name[:：]?\\s*", "");
                if (!name.isEmpty()) return name;
            }
        }
        return a.getDecision().getName() + " × " + b.getDecision().getName();
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
        return steps.isEmpty() ? List.of("执行重组方案") : steps;
    }

    private double euclideanNorm(double[] v) {
        double sum = 0;
        for (double d : v) sum += d * d;
        return Math.sqrt(sum);
    }
}
