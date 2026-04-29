package com.ontoevolve.core.spi;

import com.ontoevolve.core.model.Assignment;

import java.util.List;

/**
 * 选择算子 — 基于多目标适应度决定种群成员的存留。
 * <p>
 * 框架层级：进化层。
 * 职责：对候选方案进行 Pareto 排序与多样性维护，截断到目标容量。
 * 默认实现：ParetoCrowdingSelector (NSGA-II 风格)。
 */
public interface Selector<A extends Assignment> {
    /**
     * 从候选列表中选出 K 个个体保留在种群中，其余进入 Probation/Deprecated。
     *
     * @param candidates 候选方案（现有种群 + 新生变异）
     * @param capacity   种群容量 K
     * @return 被选中的方案列表（按 Pareto 前沿从高到低排序）
     */
    List<A> select(List<A> candidates, int capacity);
}
