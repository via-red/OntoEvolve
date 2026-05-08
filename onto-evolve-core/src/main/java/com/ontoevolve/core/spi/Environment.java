package com.ontoevolve.core.spi;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Execution;
import com.ontoevolve.core.model.Feedback;

/**
 * 环境抽象 — 评估 Assignment 执行效果，产生 Feedback。
 * <p>
 * 环境可以是人类驱动的（手动提交评估）、模拟的（启发式评分）、
 * 或外部系统驱动的（从数据库/API 获取真实效果数据）。
 */
public interface Environment {

    /** 评估已执行的方案并返回反馈评分。 */
    Feedback evaluate(Assignment assignment, Execution execution);

    /** 该环境是否自动化（无需人工参与）。 */
    default boolean isAutomated() {
        return false;
    }
}
