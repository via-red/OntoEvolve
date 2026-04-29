package com.ontoevolve.core.spi;

import com.ontoevolve.core.model.Execution;
import com.ontoevolve.core.model.Feedback;

import java.util.List;

/**
 * 信用分配器 — 将延迟到达的长期反馈分配给历史上相关的执行记录。
 * <p>
 * 框架层级：优化层。
 * 职责：当长期反馈（如学期末成绩）到达时，回溯关联的历史 Execution，
 * 沿本体因果链传播奖励。解决 RL 中延迟奖励的信用分配问题。
 */
public interface CreditAssigner {
    /**
     * 将一条反馈分配到可能相关的历史执行记录。
     *
     * @param feedback 新到达的反馈
     * @param history  历史执行记录（按时间倒序）
     * @return 更新后的反馈列表（含原始反馈和派生反馈）
     */
    List<Feedback> distribute(Feedback feedback, List<Execution> history);
}
