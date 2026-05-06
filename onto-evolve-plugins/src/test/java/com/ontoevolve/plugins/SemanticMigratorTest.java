package com.ontoevolve.plugins;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Decision;
import com.ontoevolve.plugins.migrator.SemanticMigrator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SemanticMigrator 单元测试。
 */
class SemanticMigratorTest {

    @Test
    void shouldReturnEmptyForLowCompatibility() {
        SemanticMigrator migrator = new SemanticMigrator(0.9);
        Concept source = new Concept("http://test#A", "Source");
        Concept target = new Concept("http://test#B", "Target");

        List<Assignment> migrants = migrator.proposeMigrations(source, target, List.of());
        assertTrue(migrants.isEmpty());
    }

    @Test
    void shouldMigrateBetweenParentChildConcepts() {
        SemanticMigrator migrator = new SemanticMigrator(0.5);
        Concept parent = new Concept("http://test#Parent", "Parent");
        Concept child = new Concept("http://test#Child", "Child", parent);

        Decision d = new Decision("d:1", "D1", "", List.of());
        Assignment elite = new Assignment("a:1", d, parent, 2);
        elite.setStatus(Assignment.Status.ELITE);

        List<Assignment> migrants = migrator.proposeMigrations(parent, child, List.of(elite));
        assertFalse(migrants.isEmpty());

        Assignment migrant = migrants.get(0);
        assertEquals(child, migrant.getConcept());
        assertEquals(Assignment.Status.NEWBORN, migrant.getStatus());
        assertNotNull(migrant.getParents());
    }

    @Test
    void shouldMigrateSiblingConcepts() {
        Concept grandparent = new Concept("http://test#GP", "Grandparent");
        Concept parentA = new Concept("http://test#PA", "ParentA", grandparent);
        Concept parentB = new Concept("http://test#PB", "ParentB", grandparent);

        SemanticMigrator migrator = new SemanticMigrator(0.3);
        Decision d = new Decision("d:1", "D1", "", List.of());
        Assignment elite = new Assignment("a:1", d, parentA, 2);
        elite.setStatus(Assignment.Status.ELITE);

        List<Assignment> migrants = migrator.proposeMigrations(parentA, parentB, List.of(elite));
        assertFalse(migrants.isEmpty());
    }

    @Test
    void shouldRespectThreshold() {
        SemanticMigrator migrator = new SemanticMigrator(0.99);
        Concept parent = new Concept("http://test#Parent", "Parent");
        Concept child = new Concept("http://test#Child", "Child", parent);

        Decision d = new Decision("d:1", "D1", "", List.of());
        Assignment elite = new Assignment("a:1", d, parent, 2);
        elite.setStatus(Assignment.Status.ELITE);

        List<Assignment> migrants = migrator.proposeMigrations(parent, child, List.of(elite));
        assertTrue(migrants.isEmpty());
    }
}
