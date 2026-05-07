package com.ontoevolve.graphstore.repository;

import com.ontoevolve.graphstore.node.ActionTypeNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;

public interface ActionTypeRepository extends Neo4jRepository<ActionTypeNode, String> {

    Optional<ActionTypeNode> findByIri(String iri);

    List<ActionTypeNode> findByCategory(String category);

    @Query("MATCH (t:ActionType) WHERE NOT (t)-[:SUBSUMES]->() RETURN t")
    List<ActionTypeNode> findRoots();

    @Query("MATCH (t:ActionType {iri: $iri})-[:SUBSUMES*]->(child) RETURN child")
    List<ActionTypeNode> findDescendants(String iri);
}
