package com.ontoevolve.core.model;

import com.ontoevolve.core.kernel.EvolTrace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 决策方案 — 进化的基本单元。
 * <p>
 * 每个 Decision 是一个可执行的方案模板。它由 Variator 产生，
 * 通过 Assignment 绑定到具体的 Concept 生态位。
 * Decision 本身是"基因型"——具体的演化状态由 Assignment 携带。
 */
public class Decision {
    private final String iri;
    private final String name;
    private final String description;
    private final Instant createdAt;
    private final List<String> steps;
    private final List<Decision> parents;

    public Decision(String iri, String name, String description, List<String> steps) {
        this.iri = iri;
        this.name = name;
        this.description = description;
        this.steps = steps != null ? steps : new ArrayList<>();
        this.createdAt = Instant.now();
        this.parents = new ArrayList<>();
    }

    public String getIri() { return iri; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public List<String> getSteps() { return steps; }
    public List<Decision> getParents() { return parents; }

    public void addParent(Decision parent) {
        this.parents.add(parent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Decision decision)) return false;
        return iri.equals(decision.iri);
    }

    @Override
    public int hashCode() { return iri.hashCode(); }
}
