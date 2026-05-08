package com.ontoevolve.domain.education.service;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.validation.OntologyValidator;
import com.ontoevolve.infra.store.OntologyStore;
import com.ontoevolve.infra.store.Tdb2OntologyStore;
import com.ontoevolve.infra.validation.JenaOntologyValidator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 教育领域本体验证器 — 叠加领域特有逻辑和 Jena 推理检查。
 * <p>
 * 在构造时加载 education.ttl，对每个 Assignment 执行语义检查：
 * 1. 检查 Concept IRI 是否是本体中已知的 owl:Class（含祖先回溯）
 * 2. 委托 JenaOntologyValidator 进行推理层检查（disjointness、一致性）
 * 3. 未知类型返回 "unknown" 状态而非拦截
 */
@Component
public class EducationOntologyValidator implements OntologyValidator {

    private static final Logger log = LoggerFactory.getLogger(EducationOntologyValidator.class);

    private final Model ontologyModel;
    private final JenaOntologyValidator jenaValidator;

    @Autowired
    public EducationOntologyValidator(ObjectProvider<OntologyStore> ontologyStoreProvider) {
        OntologyStore ontologyStore = ontologyStoreProvider.getIfAvailable();
        Tdb2OntologyStore tdb2Store = ontologyStore instanceof Tdb2OntologyStore ts ? ts : null;
        this.ontologyModel = tdb2Store != null && tdb2Store.getActiveModel() != null
                ? tdb2Store.getActiveModel() : loadFallbackModel();
        this.jenaValidator = tdb2Store != null ? new JenaOntologyValidator(tdb2Store) : null;
    }

    /** 包级可见，用于测试注入。 */
    EducationOntologyValidator(Model ontologyModel) {
        this.ontologyModel = ontologyModel;
        this.jenaValidator = null;
    }

    private static Model loadFallbackModel() {
        Model model = ModelFactory.createDefaultModel();
        try {
            var input = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("ontology/education.ttl");
            if (input != null) {
                model.read(input, null, "TURTLE");
            }
        } catch (Exception ignored) {
        }
        return model;
    }

    @Override
    public boolean validate(Assignment assignment) {
        if (ontologyModel.isEmpty()) {
            return true;
        }

        String conceptIri = assignment.getConcept().getIri();
        if (conceptIri == null) {
            return false;
        }

        // 1. 精确匹配 — concept IRI 是否在本体中定义为 owl:Class
        if (isKnownClass(conceptIri)) {
            return checkJena(assignment);
        }

        // 2. 祖先回溯
        Concept parent = assignment.getConcept().getParentConcept();
        while (parent != null) {
            if (isKnownClass(parent.getIri())) {
                return checkJena(assignment);
            }
            parent = parent.getParentConcept();
        }

        // 3. 未匹配到已知类型 — 记录警告但放行（LLM 动态创建的概念）
        log.warn("Concept [{}] not found in ontology TBox and no ancestor matches; " +
                        "likely dynamically created. IRI={}",
                assignment.getConcept().getLabel(), conceptIri);
        return true;
    }

    private boolean checkJena(Assignment assignment) {
        if (jenaValidator != null && !jenaValidator.validate(assignment)) {
            return false;
        }
        return true;
    }

    private boolean isKnownClass(String iri) {
        if (iri == null) return false;
        return ontologyModel.contains(ontologyModel.getResource(iri), RDF.type, OWL.Class)
                || ontologyModel.contains(ontologyModel.getResource(iri), RDF.type, RDFS.Class);
    }
}
