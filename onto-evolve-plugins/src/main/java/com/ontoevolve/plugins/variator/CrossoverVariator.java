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

/**
 * 重组变异算子 — 从种群中选择两个高分亲本，由 LLM 融合生成新方案。
 */
public class CrossoverVariator implements Variator<Decision, Assignment> {

    private final LLMClient llmClient;
    private final PromptTemplateService promptService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    public CrossoverVariator(LLMClient llmClient, PromptTemplateService promptService) {
        this.llmClient = llmClient;
        this.promptService = promptService;
    }

    @Override
    public List<Assignment> generate(VariationContext ctx) {
        List<Assignment> population = ctx.getPopulation();
        if (population.size() < 2) return List.of();

        List<Assignment> sorted = population.stream()
                .sorted(Comparator.comparingDouble(
                        a -> -euclideanNorm(a.getScoreVector())))
                .toList();

        Assignment parentA = sorted.get(0);
        int maxIndex = Math.min(3, sorted.size());
        int bIndex = random.nextInt(maxIndex - 1) + 1;
        Assignment parentB = sorted.get(bIndex);

        String prompt = promptService.render("variator_crossover", Map.of(
                "parentA", describeDecision(parentA),
                "scoreA", Arrays.toString(parentA.getScoreVector()),
                "trialsA", String.valueOf(parentA.getTrials()),
                "parentB", describeDecision(parentB),
                "scoreB", Arrays.toString(parentB.getScoreVector()),
                "trialsB", String.valueOf(parentB.getTrials()),
                "conceptLabel", ctx.getConcept().getLabel()
        ));

        LLMResponse response = llmClient.generate(prompt);
        if (response.content() == null || response.content().isBlank()) return List.of();

        Decision child = parseDecision(response.content(), parentA, parentB);
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

    private Decision parseDecision(String fusion, Assignment parentA, Assignment parentB) {
        String iri = "crossover:" + UUID.randomUUID();

        // 优先 JSON 解析
        try {
            String json = fusion;
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
                    : List.of("执行重组方案");
            return new Decision(iri,
                    name != null ? name : parentA.getDecision().getName() + " × " + parentB.getDecision().getName(),
                    desc, steps);
        } catch (Exception ignored) {
            // 回退 regex
        }

        String name = extractName(fusion, parentA, parentB);
        String desc = fusion != null && fusion.length() > 200
                ? fusion.substring(0, 200) : fusion;
        List<String> steps = extractSteps(fusion);
        return new Decision(iri, name, desc, steps);
    }

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
