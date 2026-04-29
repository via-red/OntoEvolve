package com.ontoevolve.domain.education.model;

import com.ontoevolve.core.model.Decision;

import java.util.List;

/**
 * 干预方案 — 教育领域 Decision 子类。
 * <p>
 * 如"课后谈话"、"家校通知"、"暂停参与活动"等。
 */
public class Intervention extends Decision {
    private final String interventionType; // talk / notice / activity / reward
    private final boolean requiresParentApproval;

    public Intervention(String iri, String name, String description,
                        List<String> steps, String interventionType,
                        boolean requiresParentApproval) {
        super(iri, name, description, steps);
        this.interventionType = interventionType;
        this.requiresParentApproval = requiresParentApproval;
    }

    public String getInterventionType() { return interventionType; }
    public boolean isRequiresParentApproval() { return requiresParentApproval; }
}
