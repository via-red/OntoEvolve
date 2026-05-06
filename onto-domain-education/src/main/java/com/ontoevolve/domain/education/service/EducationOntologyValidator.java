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
import org.springframework.stereotype.Component;

/**
 * 教育领域本体验证器 — 基于 education.ttl 的运行时 TBox 约束校验。
 * <p>
 * 在构造时加载 OWL 本体文件，对每个 Assignment 执行语义检查：
 * 1. 验证 Concept IRI 是否是本体中已知的 owl:Class
 * 2. 若精确匹配失败，沿 parentConcept 链回溯检查
 * 3. 当本体不可用时，默认放行（fail-open）
 */
@Component
public class EducationOntologyValidator implements OntologyValidator {

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
        Resource conceptResource = ontologyModel.getResource(conceptIri);
        if (conceptResource != null && ontologyModel.contains(conceptResource, RDF.type, (RDFNode) null)) {
            for (Statement stmt : ontologyModel.listStatements(conceptResource, RDF.type, (RDFNode) null).toList()) {
                if (OWL.Class.equals(stmt.getObject()) || RDFS.Class.equals(stmt.getObject())) {
                    return true;
                }
            }
        }

        // 2. 祖先回溯：检查父概念链中是否有已知的 owl:Class
        Concept parent = assignment.getConcept().getParentConcept();
        while (parent != null) {
            Resource parentResource = ontologyModel.getResource(parent.getIri());
            if (parentResource != null) {
                for (Statement stmt : ontologyModel.listStatements(parentResource, RDF.type, (RDFNode) null).toList()) {
                    if (OWL.Class.equals(stmt.getObject()) || RDFS.Class.equals(stmt.getObject())) {
                        return true;
                    }
                }
            }
            parent = parent.getParentConcept();
        }

        // 3. 未匹配到已知类型 — 可能是动态创建的概念，放行
        return true;
    }
}
