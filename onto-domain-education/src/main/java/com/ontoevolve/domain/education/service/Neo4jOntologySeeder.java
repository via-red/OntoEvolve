package com.ontoevolve.domain.education.service;

import com.ontoevolve.domain.education.model.ActionType;
import com.ontoevolve.graphstore.node.ActionTypeNode;
import com.ontoevolve.graphstore.repository.ActionTypeRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the Neo4j ontology tree from EducationOntologyService on startup.
 * Only active when {@code onto.graph.store-type=neo4j}.
 */
@Component
@ConditionalOnProperty(prefix = "onto.graph", name = "store-type", havingValue = "neo4j")
public class Neo4jOntologySeeder {

    private static final Logger log = LoggerFactory.getLogger(Neo4jOntologySeeder.class);

    private final EducationOntologyService ontologyService;
    private final ActionTypeRepository actionTypeRepo;

    public Neo4jOntologySeeder(EducationOntologyService ontologyService,
                               ActionTypeRepository actionTypeRepo) {
        this.ontologyService = ontologyService;
        this.actionTypeRepo = actionTypeRepo;
    }

    @PostConstruct
    public void seed() {
        List<ActionTypeNode> existing = actionTypeRepo.findRoots();
        if (!existing.isEmpty()) {
            log.info("Neo4j ontology already seeded ({} roots found)", existing.size());
            return;
        }

        log.info("Seeding Neo4j ontology tree from EducationOntologyService...");
        for (ActionType type : ontologyService.getAllActionTypes()) {
            persistWithParent(type);
        }
        log.info("Neo4j ontology seeded successfully");
    }

    private void persistWithParent(ActionType type) {
        if (actionTypeRepo.findById(type.getIri()).isPresent()) return;

        ActionTypeNode node = new ActionTypeNode(
                type.getIri(),
                type.getLabel(),
                type.getCategory(),
                (String) type.getProperties().getOrDefault("comment", "")
        );

        if (type.hasParent() && type.getParentConcept() instanceof ActionType parent) {
            persistWithParent(parent);
            actionTypeRepo.findById(parent.getIri())
                    .ifPresent(node::setParent);
        }

        actionTypeRepo.save(node);
    }
}
