package com.ontoevolve.plugins.migrator;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.spi.Migrator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 语义迁移算子 — 基于本体兼容性的跨生态位迁移。
 * <p>
 * 评估源生态位与目标生态位的语义相似性（共享父类、适用属性重叠），
 * 当兼容度超过阈值时，复制精英方案到目标生态位。
 */
public class SemanticMigrator implements Migrator<Assignment> {

    private final double compatibilityThreshold;

    public SemanticMigrator(double compatibilityThreshold) {
        this.compatibilityThreshold = compatibilityThreshold;
    }

    public SemanticMigrator() {
        this(0.85);
    }

    @Override
    public List<Assignment> proposeMigrations(Concept source, Concept target,
                                              List<Assignment> sourceElites) {
        List<Assignment> migrants = new ArrayList<>();
        double compatibility = computeCompatibility(source, target);

        if (compatibility < compatibilityThreshold) return migrants;

        for (Assignment elite : sourceElites) {
            Assignment migrant = new Assignment(
                    "migrant:" + UUID.randomUUID(),
                    elite.getDecision(),
                    target,
                    elite.getScoreVector().length
            );
            migrant.setParents(List.of(elite));
            migrant.setStatus(Assignment.Status.NEWBORN);
            migrants.add(migrant);
        }

        return migrants;
    }

    /**
     * 计算两个 Concept 之间的语义兼容性。
     * <p>
     * 基于共享父概念、本体属性重叠度等指标。
     * 工程实现中应集成嵌入相似度计算或 SPARQL 查询。
     */
    private double computeCompatibility(Concept source, Concept target) {
        if (source.equals(target)) return 1.0;
        if (source.getIri().equals(target.getIri())) return 1.0;

        double score = 0.0;

        // 1. 直接父子关系
        if (source.getParentConcept() != null
                && source.getParentConcept().equals(target)) {
            score += 0.6;
        }
        if (target.getParentConcept() != null
                && target.getParentConcept().equals(source)) {
            score += 0.6;
        }

        // 2. 共享祖先
        if (shareAncestor(source, target)) {
            score += 0.3;
        }

        // 3. 属性重叠度
        score += computePropertyOverlap(source, target) * 0.1;

        return Math.min(1.0, score);
    }

    private boolean shareAncestor(Concept c1, Concept c2) {
        return findAncestors(c1).stream().anyMatch(
                a -> findAncestors(c2).contains(a));
    }

    private List<Concept> findAncestors(Concept c) {
        List<Concept> ancestors = new ArrayList<>();
        Concept current = c.getParentConcept();
        while (current != null) {
            ancestors.add(current);
            current = current.getParentConcept();
        }
        return ancestors;
    }

    private double computePropertyOverlap(Concept source, Concept target) {
        var srcProps = source.getProperties().keySet();
        var tgtProps = target.getProperties().keySet();
        if (srcProps.isEmpty() || tgtProps.isEmpty()) return 0.5;

        long overlap = srcProps.stream().filter(tgtProps::contains).count();
        return (double) overlap / Math.max(srcProps.size(), tgtProps.size());
    }
}
