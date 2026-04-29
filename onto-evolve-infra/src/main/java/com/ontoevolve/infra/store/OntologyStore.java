package com.ontoevolve.infra.store;

import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.model.Feedback;

import java.util.List;
import java.util.Optional;

/**
 * 本体存储抽象接口 — 屏蔽底层 RDF 存储差异。
 * <p>
 * 支持 TDB2、RDF4J、内存等不同后端。
 * 核心操作：概念查询、方案读写、反馈写入、进化轨迹持久化。
 */
public interface OntologyStore {
    /** 初始化存储（加载本体文件、建立推理模型） */
    void initialize(String ontologyPath, String baseNamespace, String inferenceMode);

    // --- Concept ---
    Optional<Concept> findConcept(String iri);
    List<Concept> findConceptsByParent(String parentIri);
    List<Concept> getAllConcepts();
    void saveConcept(Concept concept);

    // --- Decision ---
    void saveDecision(Decision decision);
    Optional<Decision> findDecision(String iri);

    // --- Feedback ---
    void saveFeedback(Feedback feedback);
    List<Feedback> findFeedbacksByExecution(String executionIri);
    long countFeedbacksByConcept(String conceptIri);

    // --- SPARQL ---
    <T> List<T> query(String sparql, Class<T> resultType);

    /** 关闭存储 */
    void close();
}
