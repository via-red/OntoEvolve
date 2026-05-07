package com.ontoevolve.graphstore.repository;

import com.ontoevolve.graphstore.node.ActionEventNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface ActionEventRepository extends Neo4jRepository<ActionEventNode, String> {

    Optional<ActionEventNode> findByEventId(String eventId);
}
