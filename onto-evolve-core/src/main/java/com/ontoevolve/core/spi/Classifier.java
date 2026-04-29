package com.ontoevolve.core.spi;

import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.InputEvent;

/**
 * 输入分类器 — 将原始输入映射到本体概念。
 * <p>
 * 框架层级：输入层。
 * 职责：将 InputEvent 分类为对应的 Concept 生态位。
 * 默认方案：LLM 驱动的分类器，利用语义理解能力处理开放式文本。
 */
public interface Classifier<I extends InputEvent, C extends Concept> {
    /**
     * 对输入事件进行分类，返回所属的本体概念。
     */
    C classify(I event);

    /**
     * 是否支持未知类别自动创建（暂存到提案队列）。
     */
    default boolean supportsUnknown() {
        return false;
    }
}
