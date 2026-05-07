package com.ontoevolve.graphstore.repository;

import com.ontoevolve.graphstore.node.InterventionNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface InterventionRepository extends Neo4jRepository<InterventionNode, String> {

    Optional<InterventionNode> findByIri(String iri);
}
