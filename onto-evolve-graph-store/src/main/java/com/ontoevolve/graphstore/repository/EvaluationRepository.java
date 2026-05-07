package com.ontoevolve.graphstore.repository;

import com.ontoevolve.graphstore.node.EvaluationNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface EvaluationRepository extends Neo4jRepository<EvaluationNode, String> {

    Optional<EvaluationNode> findByIri(String iri);
}
