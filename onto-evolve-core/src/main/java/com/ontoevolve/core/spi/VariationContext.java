package com.ontoevolve.core.spi;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;

import java.util.List;
import java.util.Map;

/**
 * 变异上下文 — 传递给 Variator 的生态位快照。
 * <p>
 * 包含当前生态位的概念、种群成员和进化参数，
 * 使变异算子能够基于生态位状态做出有偏向性的变异。
 */
public class VariationContext {
    private final Concept concept;
    private final List<Assignment> population;
    private final Map<String, Object> params;
    private final double explorationRate;

    public VariationContext(Concept concept, List<Assignment> population,
                           Map<String, Object> params, double explorationRate) {
        this.concept = concept;
        this.population = population;
        this.params = params;
        this.explorationRate = explorationRate;
    }

    public Concept getConcept() { return concept; }
    public List<Assignment> getPopulation() { return population; }
    public Map<String, Object> getParams() { return params; }
    public double getExplorationRate() { return explorationRate; }
}
