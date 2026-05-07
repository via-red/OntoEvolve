package com.ontoevolve.domain.education.service;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.validation.OntologyValidator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 教育领域本体验证器 — 基于 education.ttl 的运行时 TBox 约束校验。
 * <p>
 * 在构造时加载 OWL 本体文件，对每个 Assignment 执行语义检查：
 * 1. 验证 Concept IRI 是否是本体中已知的 owl:Class
 * 2. 若精确匹配失败，沿 parentConcept 链回溯检查
 * 3. 若仍未匹配到已知类型（如 LLM 动态创建的概念），记录警告日志但放行
 */
@Component
public class EducationOntologyValidator implements OntologyValidator {

    private static final Logger log = LoggerFactory.getLogger(EducationOntologyValidator.class);

    private final Model ontologyModel;

    public EducationOntologyValidator() {
        this.ontologyModel = loadOntology();
    }

    /** 包级可见，用于测试注入 */
    EducationOntologyValidator(Model ontologyModel) {
        this.ontologyModel = ontologyModel;
    }

    private static Model loadOntology() {
        Model model = ModelFactory.createDefaultModel();
        try {
            var input = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("ontology/education.ttl");
            if (input != null) {
                model.read(input, null, "TURTLE");
            }
        } catch (Exception ignored) {
            // 本体加载失败时使用空模型，所有验证放行
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
            return true;
        }

        // 1. 精确匹配：concept IRI 是否在本体中定义为 owl:Class
        if (isKnownClass(conceptIri)) {
            return true;
        }

        // 2. 祖先回溯：检查父概念链中是否有已知的 owl:Class
        Concept parent = assignment.getConcept().getParentConcept();
        while (parent != null) {
            if (isKnownClass(parent.getIri())) {
                return true;
            }
            parent = parent.getParentConcept();
        }

        // 3. 未匹配到已知类型 — 可能是 LLM 动态创建的概念，放行并警告
        log.warn("Concept [{}] not found in ontology TBox and no ancestor matches; " +
                        "likely dynamically created. IRI={}",
                assignment.getConcept().getLabel(), conceptIri);
        return true;
    }

    private boolean isKnownClass(String iri) {
        Resource resource = ontologyModel.getResource(iri);
        if (resource == null) return false;
        for (Statement stmt : ontologyModel.listStatements(resource, RDF.type, (RDFNode) null).toList()) {
            if (OWL.Class.equals(stmt.getObject()) || RDFS.Class.equals(stmt.getObject())) {
                return true;
            }
        }
        return false;
    }
}
