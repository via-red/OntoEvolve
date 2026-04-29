package com.ontoevolve.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 本体概念 — 系统的生态位地图节点。
 * <p>
 * 每个 Concept 对应一个语义类别，通过 parentConcept 形成 is-a 层次。
 * 每个 Concept 下挂载一个 DecisionPopulation，维护该生态位的方案种群。
 */
public class Concept {
    private final String iri;
    private final String label;
    private final Concept parentConcept;
    private final Map<String, Object> properties;

    public Concept(String iri, String label) {
        this(iri, label, null);
    }

    public Concept(String iri, String label, Concept parentConcept) {
        this.iri = iri;
        this.label = label;
        this.parentConcept = parentConcept;
        this.properties = new HashMap<>();
    }

    public String getIri() { return iri; }
    public String getLabel() { return label; }
    public Concept getParentConcept() { return parentConcept; }
    public Map<String, Object> getProperties() { return properties; }

    public boolean hasParent() { return parentConcept != null; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Concept concept)) return false;
        return iri.equals(concept.iri);
    }

    @Override
    public int hashCode() { return iri.hashCode(); }
}
