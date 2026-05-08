package com.ontoevolve.infra.validation;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.validation.OntologyValidator;
import com.ontoevolve.infra.store.Tdb2OntologyStore;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Jena 驱动的本体验证器 — 基于 InfModel 进行语义安全校验。
 * <p>
 * 检查：OWL 类存在性、父层次回溯、disjointness 冲突、模型一致性。
 * 使用 Jena Core API（无需 jena-ontology 依赖）。
 */
public class JenaOntologyValidator implements OntologyValidator {

    private static final Logger log = LoggerFactory.getLogger(JenaOntologyValidator.class);

    private final Tdb2OntologyStore ontologyStore;

    public JenaOntologyValidator(Tdb2OntologyStore ontologyStore) {
        this.ontologyStore = ontologyStore;
    }

    @Override
    public boolean validate(Assignment assignment) {
        Concept concept = assignment.getConcept();
        if (concept == null || concept.getIri() == null) {
            return false;
        }

        Model model = ontologyStore.getActiveModel();
        if (model == null || model.isEmpty()) {
            return true;
        }

        // 1. 检查 OWL 类存在性
        if (!isKnownClass(model, concept.getIri())) {
            Concept parent = concept.getParentConcept();
            boolean foundAncestor = false;
            while (parent != null) {
                if (isKnownClass(model, parent.getIri())) {
                    foundAncestor = true;
                    break;
                }
                parent = parent.getParentConcept();
            }
            if (!foundAncestor) {
                log.warn("Ontology validation FAILED: concept [{}] IRI={} not found in TBox and no ancestor matches",
                        concept.getLabel(), concept.getIri());
                return false;
            }
        }

        // 2. Disjointness 检查
        if (!checkDisjointness(model, concept.getIri())) {
            log.warn("Ontology validation FAILED: concept [{}] violates disjointness constraints",
                    concept.getLabel());
            return false;
        }

        // 3. 一致性检查（仅 owl 推理模式）
        if ("owl".equals(ontologyStore.getInferenceMode())) {
            ValidityReport report = ontologyStore.validate();
            if (report != null && !report.isValid()) {
                log.warn("Ontology validation FAILED: model consistency violation");
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean validate(com.ontoevolve.core.model.Decision decision) {
        return decision != null && decision.getName() != null && !decision.getName().isBlank();
    }

    private boolean isKnownClass(Model model, String iri) {
        if (iri == null) return false;
        return model.contains(model.getResource(iri), RDF.type, OWL.Class)
                || model.contains(model.getResource(iri), RDF.type, RDFS.Class);
    }

    /** 检查 concept IRI 是否不违反 disjointness 约束。 */
    private boolean checkDisjointness(Model model, String conceptIri) {
        try {
            var resource = model.getResource(conceptIri);
            if (resource == null) return true;

            // 收集所有 disjoint 的类
            Set<String> disjointClasses = new HashSet<>();
            for (Statement stmt : model.listStatements(resource, OWL.disjointWith, (RDFNode) null).toList()) {
                if (stmt.getObject().isResource()) {
                    disjointClasses.add(stmt.getObject().asResource().getURI());
                }
            }

            if (disjointClasses.isEmpty()) return true;

            // 收集当前类的所有父类（含传递）
            Set<String> superClasses = new HashSet<>();
            collectSuperClasses(model, conceptIri, superClasses);

            // 检查是否有父类同时也在 disjoint 列表中
            for (String sc : superClasses) {
                if (disjointClasses.contains(sc)) {
                    return false; // 违反互斥约束
                }
            }
        } catch (Exception e) {
            log.debug("Disjointness check skipped for {}: {}", conceptIri, e.getMessage());
        }
        return true;
    }

    /** 递归收集所有父类 IRI（rdfs:subClassOf 传递闭包）。 */
    private void collectSuperClasses(Model model, String iri, Set<String> result) {
        var resource = model.getResource(iri);
        if (resource == null) return;
        for (Statement stmt : model.listStatements(resource, RDFS.subClassOf, (RDFNode) null).toList()) {
            if (stmt.getObject().isResource()) {
                String superIri = stmt.getObject().asResource().getURI();
                if (result.add(superIri)) {
                    collectSuperClasses(model, superIri, result);
                }
            }
        }
    }
}
