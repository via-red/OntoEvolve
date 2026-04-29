package com.ontoevolve.core.kernel;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 方案种群 — 挂载在一个 Concept 下的活跃方案集合。
 * <p>
 * 种群通过选择压力维持多样性，包含容量控制和 Pareto 前沿排序。
 * 这是"生态位"的具体承载者，也是进化发生的场所。
 */
public class DecisionPopulation {
    private final Concept concept;
    private final int maxSize;
    private final List<Assignment> members;
    private int generationCounter;

    public DecisionPopulation(Concept concept, int maxSize) {
        this.concept = concept;
        this.maxSize = maxSize;
        this.members = new ArrayList<>();
        this.generationCounter = 0;
    }

    public Concept getConcept() { return concept; }
    public int getMaxSize() { return maxSize; }
    public int getGenerationCounter() { return generationCounter; }
    public void incrementGeneration() { generationCounter++; }

    public List<Assignment> getActiveMembers() {
        return members.stream()
                .filter(a -> a.getStatus() == Assignment.Status.ACTIVE
                        || a.getStatus() == Assignment.Status.ELITE
                        || a.getStatus() == Assignment.Status.NEWBORN)
                .toList();
    }

    public List<Assignment> getAllMembers() {
        return Collections.unmodifiableList(members);
    }

    public void addMember(Assignment assignment) {
        assignment.setGeneration(generationCounter);
        assignment.setStatus(Assignment.Status.NEWBORN);
        members.add(assignment);
    }

    /** 替换整个种群成员（由选择算子执行后调用） */
    public void replaceMembers(List<Assignment> newMembers) {
        members.clear();
        members.addAll(newMembers);
    }

    /** 获取当前种群容量 */
    public int size() { return members.size(); }

    /** 检查是否需要触发进化重排（反馈数 vs 种群容量） */
    public boolean isEvolutionDue(int feedbackCount) {
        return feedbackCount >= maxSize && !members.isEmpty();
    }
}
