package com.ontoevolve.plugins.variator;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.spi.Variator;
import com.ontoevolve.core.spi.VariationContext;

import java.util.*;

/**
 * 微扰变异算子 — 对现有方案进行小幅参数调整。
 * <p>
 * 不依赖 LLM，成本极低，用于在参数空间中局部搜索。
 * 可用于调整数值型属性（如通知时机、阈值等）。
 */
public class PerturbVariator implements Variator<Decision, Assignment> {

    private final Random random;

    public PerturbVariator() {
        this.random = new Random();
    }

    @Override
    public List<Assignment> generate(VariationContext ctx) {
        List<Assignment> population = ctx.getPopulation();
        if (population.isEmpty()) return List.of();

        Assignment parent = population.get(random.nextInt(population.size()));
        Decision parentDecision = parent.getDecision();

        String perturbedName = parentDecision.getName() + " (微调)";
        List<String> perturbedSteps = new ArrayList<>(parentDecision.getSteps());
        if (!perturbedSteps.isEmpty()) {
            int idx = random.nextInt(perturbedSteps.size());
            String step = perturbedSteps.get(idx);
            perturbedSteps.set(idx, applyPerturbation(step));
        }

        Decision child = new Decision(
                "perturb:" + UUID.randomUUID(),
                perturbedName,
                parentDecision.getDescription() + " [微调变异]",
                perturbedSteps
        );
        child.addParent(parentDecision);

        Assignment childAssign = new Assignment(
                "asgn:" + UUID.randomUUID(),
                child,
                ctx.getConcept(),
                parent.getScoreVector().length
        );
        childAssign.setParents(List.of(parent));
        childAssign.setGeneration(parent.getGeneration() + 1);

        return List.of(childAssign);
    }

    @Override
    public String type() { return "PERTURB"; }

    private String applyPerturbation(String step) {
        // 简单微扰：替换数值或添加修饰词
        if (step.matches(".*\\d+.*")) {
            return step.replaceAll("\\d+", String.valueOf(random.nextInt(30) + 1));
        }
        List<String> modifiers = List.of("适当", "酌情", "灵活地", "严格地", "分阶段");
        String mod = modifiers.get(random.nextInt(modifiers.size()));
        return mod + step;
    }
}
