package com.ontoevolve.graphstore.repository;

import com.ontoevolve.graphstore.node.EvolTraceNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface EvolTraceRepository extends Neo4jRepository<EvolTraceNode, String> {

    Optional<EvolTraceNode> findById(String id);
}
