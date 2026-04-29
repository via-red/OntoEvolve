package com.ontoevolve.core.spi;

import java.util.Map;

/**
 * 元优化器 — 调整进化引擎自身的超参数。
 * <p>
 * 框架层级：元进化层。
 * 职责：以全局系统指标为目标，自适应调整变异率、种群容量等参数。
 * 这是"进化进化能力"的关键接口。
 */
public interface MetaOptimizer {
    /**
     * 基于当前全局指标，计算下一周期的超参数。
     *
     * @param metrics 全局系统指标快照
     * @return 调整后的超参数字典
     */
    Map<String, Double> optimize(GlobalMetrics metrics);
}
