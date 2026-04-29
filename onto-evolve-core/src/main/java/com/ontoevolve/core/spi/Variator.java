package com.ontoevolve.core.spi;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Decision;

import java.util.List;

/**
 * 变异算子 — 产生新方案，为种群注入多样性。
 * <p>
 * 框架层级：进化层。
 * 职责：基于当前种群和生态位特征，产生新的 Assignment。
 * 框架内置三种实现：LLM_Generate, Crossover, Perturb。
 * 领域项目可自定义变异策略。
 */
public interface Variator<D extends Decision, A extends Assignment> {
    /**
     * 执行变异，产生新方案列表。
     */
    List<A> generate(VariationContext ctx);

    /**
     * 变异算子类型标识，用于配置路由。
     */
    String type();
}
