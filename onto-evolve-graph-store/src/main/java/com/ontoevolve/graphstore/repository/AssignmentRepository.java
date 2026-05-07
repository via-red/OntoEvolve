package com.ontoevolve.graphstore.repository;

import com.ontoevolve.graphstore.node.AssignmentNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends Neo4jRepository<AssignmentNode, String> {

    Optional<AssignmentNode> findByIri(String iri);

    @Query("MATCH (a:Assignment)-[:FOR_CONCEPT]->(t:ActionType {iri: $conceptIri}) RETURN a ORDER BY a.effectiveness DESC")
    List<AssignmentNode> findByConceptIriOrderByEffectivenessDesc(String conceptIri);

    @Query("MATCH (a:Assignment)-[:FOR_CONCEPT]->(t:ActionType {iri: $conceptIri}) RETURN a")
    List<AssignmentNode> findByConceptIri(String conceptIri);

    @Query("MATCH (a:Assignment) WHERE a.status IN $statuses RETURN a")
    List<AssignmentNode> findByStatusIn(List<String> statuses);
}
