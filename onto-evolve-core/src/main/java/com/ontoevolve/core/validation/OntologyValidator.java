package com.ontoevolve.core.validation;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Decision;

/**
 * 本体验证器 — 所有变异和迁移操作的语义安全闸。
 * <p>
 * 验证新生成的方案和分配是否满足本体 TBox 约束：
 * - 类层次合法性（Decision 必须是正确子类）
 * - 属性约束（domain/range）
 * - 自定义业务规则（如"不对低年级使用停学"）
 * - 互斥类检查（disjointness）
 */
public interface OntologyValidator {
    /**
     * 验证一个 Assignment 是否符合本体约束。
     * 在变异后、迁移后、持久化前调用。
     */
    boolean validate(Assignment assignment);

    /**
     * 验证一个 Decision 在语义上是否合法。
     */
    default boolean validate(Decision decision) {
        return true;
    }
}
