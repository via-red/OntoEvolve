package com.ontoevolve.core.spi;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;

import java.util.List;
import java.util.Map;

/**
 * 决策匹配器 — 从生态位的方案种群中选出最优分配。
 * <p>
 * 框架层级：决策层。
 * 职责：给定 Concept 和上下文，从 DecisionPopulation 中选出最优 Assignment。
 * 默认实现：Pareto 排序 + UCB 探索的混合策略。
 */
public interface Matcher<C extends Concept, D extends Decision, A extends Assignment> {
    /**
     * 为给定的概念和上下文匹配最优方案分配。
     */
    A match(C concept, Context ctx);

    /**
     * 带种群感知的匹配 — 默认退化为无视种群的普通匹配。
     * 实现类可覆盖此方法以利用种群信息（如 ParetoUCB 的探索因子）。
     */
    default A match(C concept, Context ctx, List<A> population) {
        return match(concept, ctx);
    }

    class Context {
        private final String subjectId;
        private final Map<String, Object> params;

        public Context(String subjectId, Map<String, Object> params) {
            this.subjectId = subjectId;
            this.params = params;
        }

        public String getSubjectId() { return subjectId; }
        public Map<String, Object> getParams() { return params; }
    }
}
