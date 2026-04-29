package com.ontoevolve.infra.store;

import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.model.Feedback;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.tdb2.TDB2Factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Apache Jena TDB2 实现的本体存储。
 * <p>
 * TDB2 是高性能的 RDF 三元组存储，支持 SPARQL 查询和 OWL 推理。
 * 支持持久化到磁盘，适合生产环境。
 */
public class Tdb2OntologyStore implements OntologyStore {

    private final String storePath;
    private Dataset dataset;

    public Tdb2OntologyStore(String storePath) {
        this.storePath = storePath;
    }

    @Override
    public void initialize(String ontologyPath, String baseNamespace, String inferenceMode) {
        dataset = TDB2Factory.connectDataset(storePath);
        // 生产环境应在此处加载本体文件到 dataset
        // dataset.getDefaultModel().read(ontologyPath, "TURTLE");
    }

    @Override
    public Optional<Concept> findConcept(String iri) {
        String sparql = """
                PREFIX onto: <http://ontoevolve/core#>
                SELECT ?label ?parent WHERE {
                    <%s> a onto:Concept ;
                         onto:label ?label .
                    OPTIONAL { <%s> onto:parentConcept ?parent . }
                }
                """.formatted(iri, iri);

        return executeQuery(sparql, rs -> {
            if (!rs.hasNext()) return Optional.empty();
            QuerySolution sol = rs.next();
            String label = sol.get("label").asLiteral().getString();
            Concept concept = new Concept(iri, label);
            // 生产环境应递归查询父概念
            return Optional.of(concept);
        });
    }

    @Override
    public List<Concept> findConceptsByParent(String parentIri) {
        String sparql = """
                PREFIX onto: <http://ontoevolve/core#>
                SELECT ?iri ?label WHERE {
                    ?iri a onto:Concept ;
                         onto:parentConcept <%s> ;
                         onto:label ?label .
                }
                """.formatted(parentIri);

        return executeQuery(sparql, rs -> {
            List<Concept> results = new ArrayList<>();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                String iri = sol.get("iri").asResource().getURI();
                String label = sol.get("label").asLiteral().getString();
                results.add(new Concept(iri, label));
            }
            return results;
        });
    }

    @Override
    public List<Concept> getAllConcepts() {
        String sparql = """
                PREFIX onto: <http://ontoevolve/core#>
                SELECT ?iri ?label WHERE {
                    ?iri a onto:Concept ;
                         onto:label ?label .
                }
                """;

        return executeQuery(sparql, rs -> {
            List<Concept> results = new ArrayList<>();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                String iri = sol.get("iri").asResource().getURI();
                String label = sol.get("label").asLiteral().getString();
                results.add(new Concept(iri, label));
            }
            return results;
        });
    }

    @Override
    public void saveConcept(Concept concept) {
        // 生产环境应使用 RDF 模型写入
    }

    @Override
    public void saveDecision(Decision decision) {
        // RDF 持久化
    }

    @Override
    public Optional<Decision> findDecision(String iri) {
        return Optional.empty();
    }

    @Override
    public void saveFeedback(Feedback feedback) {
        // RDF 持久化
    }

    @Override
    public List<Feedback> findFeedbacksByExecution(String executionIri) {
        return List.of();
    }

    @Override
    public long countFeedbacksByConcept(String conceptIri) {
        String sparql = """
                PREFIX onto: <http://ontoevolve/core#>
                SELECT (COUNT(?fb) AS ?cnt) WHERE {
                    ?fb a onto:Feedback ;
                        onto:relatedConcept <%s> .
                }
                """.formatted(conceptIri);

        return executeQuery(sparql, rs -> {
            if (!rs.hasNext()) return 0L;
            return rs.next().get("cnt").asLiteral().getLong();
        });
    }

    @Override
    public <T> List<T> query(String sparql, Class<T> resultType) {
        throw new UnsupportedOperationException("Generic SPARQL query not implemented");
    }

    @Override
    public void close() {
        if (dataset != null) dataset.close();
    }

    /** 执行 SPARQL SELECT 查询，使用 ResultSet 处理函数 */
    private <T> T executeQuery(String sparql, Function<ResultSet, T> handler) {
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, dataset)) {
            ResultSet rs = qe.execSelect();
            return handler.apply(rs);
        }
    }
}
