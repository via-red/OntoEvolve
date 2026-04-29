package com.ontoevolve.domain.education.model;

import com.ontoevolve.core.model.Concept;

/**
 * 行为类型 — 教育领域 Concept 子类。
 * <p>
 * 代表一个行为生态位，如"课堂扰乱"、"作业不交"、"网络欺凌"。
 */
public class ActionType extends Concept {
    private final String category; // academic / behavioral / social

    public ActionType(String iri, String label, ActionType parent, String category) {
        super(iri, label, parent);
        this.category = category;
        getProperties().put("category", category);
    }

    public String getCategory() { return category; }
}
