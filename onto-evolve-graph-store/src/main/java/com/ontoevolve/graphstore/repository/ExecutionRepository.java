package com.ontoevolve.graphstore.repository;

import com.ontoevolve.graphstore.node.ExecutionNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface ExecutionRepository extends Neo4jRepository<ExecutionNode, String> {

    Optional<ExecutionNode> findByIri(String iri);
}
