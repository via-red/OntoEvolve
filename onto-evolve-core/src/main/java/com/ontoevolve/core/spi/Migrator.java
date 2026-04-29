package com.ontoevolve.core.spi;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;

import java.util.List;

/**
 * 迁移算子 — 跨生态位的知识复用。
 * <p>
 * 框架层级：进化层。
 * 职责：在语义兼容的 Concept 之间迁移精英方案，
 * 模拟生物学中的水平基因转移。
 */
public interface Migrator<A extends Assignment> {
    /**
     * 评估从源生态位到目标生态位的迁移候选。
     *
     * @param source      源生态位
     * @param target      目标生态位
     * @param sourceElites 源生态位的精英方案
     * @return 可迁移到目标生态位的方案列表
     */
    List<A> proposeMigrations(Concept source, Concept target, List<A> sourceElites);
}
