package com.ontoevolve.infra.store;

import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.core.model.Feedback;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.tdb2.TDB2Factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Apache Jena TDB2 实现的本体存储。
 * <p>
 * 支持 OWL/RDFS 推理（通过 InfModel），推理模式可通过配置切换。
 */
public class Tdb2OntologyStore implements OntologyStore {

    private final String storePath;
    private Dataset dataset;
    private Model baseModel;
    private InfModel infModel;
    private String inferenceMode;

    public Tdb2OntologyStore(String storePath) {
        this.storePath = storePath;
    }

    @Override
    public void initialize(String ontologyPath, String baseNamespace, String inferenceMode) {
        this.inferenceMode = inferenceMode;
        dataset = TDB2Factory.connectDataset(storePath);

        String resolvedPath = ontologyPath;
        if (ontologyPath != null && ontologyPath.startsWith("classpath:")) {
            String resourcePath = ontologyPath.substring("classpath:".length());
            var resource = getClass().getClassLoader().getResource(resourcePath);
            if (resource != null) {
                resolvedPath = resource.toString();
            }
        }

        baseModel = dataset.getDefaultModel();
        baseModel.read(resolvedPath, "TURTLE");

        if (inferenceMode == null || "none".equalsIgnoreCase(inferenceMode)) {
            infModel = null;
        } else if ("rdfs".equalsIgnoreCase(inferenceMode)) {
            Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
            infModel = ModelFactory.createInfModel(reasoner, baseModel);
        } else if ("owl".equalsIgnoreCase(inferenceMode)) {
            Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
            infModel = ModelFactory.createInfModel(reasoner, baseModel);
        }
    }

    /** 获取基础 RDF 模型。 */
    public Model getBaseModel() {
        return baseModel;
    }

    /** 获取推理模型（仅 rdfs/owl 模式，否则 null）。 */
    public InfModel getInfModel() {
        return infModel;
    }

    /** 获取活跃模型（优先 InfModel，回退 baseModel）。 */
    public Model getActiveModel() {
        return infModel != null ? infModel : baseModel;
    }

    /** 验证本体一致性。 */
    public ValidityReport validate() {
        if (infModel != null) {
            return infModel.validate();
        }
        return null;
    }

    public String getInferenceMode() {
        return inferenceMode;
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
            return Optional.of(new Concept(iri, label));
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
    }

    @Override
    public void saveDecision(Decision decision) {
    }

    @Override
    public Optional<Decision> findDecision(String iri) {
        return Optional.empty();
    }

    @Override
    public void saveFeedback(Feedback feedback) {
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
        if (infModel != null) infModel.close();
        if (dataset != null) dataset.close();
    }

    private <T> T executeQuery(String sparql, Function<ResultSet, T> handler) {
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, dataset)) {
            ResultSet rs = qe.execSelect();
            return handler.apply(rs);
        }
    }
}
