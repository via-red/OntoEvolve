package com.ontoevolve.graphstore.config;

import com.ontoevolve.core.spi.PopulationStore;
import com.ontoevolve.graphstore.repository.*;
import com.ontoevolve.graphstore.store.Neo4jPopulationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 * Auto-configuration for Neo4j-backed graph store.
 *
 * <p>Activated when {@code onto.graph.store-type=neo4j}.
 * Requires Spring Data Neo4j on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.data.neo4j.repository.Neo4jRepository")
@ConditionalOnProperty(prefix = "onto.graph", name = "store-type", havingValue = "neo4j")
@EnableNeo4jRepositories(basePackageClasses = {
        ActionTypeRepository.class,
        InterventionRepository.class,
        AssignmentRepository.class,
        ExecutionRepository.class,
        EvaluationRepository.class,
        ActionEventRepository.class,
        EvolTraceRepository.class
})
public class GraphStoreAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GraphStoreAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "onto.graph", name = "store-type", havingValue = "neo4j")
    public PopulationStore neo4jPopulationStore(AssignmentRepository assignmentRepo,
                                                 ActionTypeRepository actionTypeRepo,
                                                 InterventionRepository interventionRepo,
                                                 EvolTraceRepository traceRepo) {
        log.info("Initializing Neo4jPopulationStore — graph store mode");
        return new Neo4jPopulationStore(
                assignmentRepo, actionTypeRepo, interventionRepo, traceRepo);
    }
}
