package com.ontoevolve.domain.education;

import com.ontoevolve.domain.education.model.ActionType;
import com.ontoevolve.domain.education.service.EducationOntologyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EducationOntologyServiceTest {

    private static final String NS = "http://ontoevolve/education#";

    private EducationOntologyService service;

    @BeforeEach
    void setUp() {
        service = new EducationOntologyService();
    }

    @Test
    void shouldInitializeWithFallbackTypes() {
        List<ActionType> all = service.getAllActionTypes();
        // At minimum the 7 fallback types (3 roots + 4 children) should exist
        assertTrue(all.size() >= 7, "Should have at least 7 fallback types, got " + all.size());
    }

    @Test
    void shouldHaveRootCategories() {
        assertTrue(service.findActionType(NS + "Behavioral").isPresent(), "Behavioral root missing");
        assertTrue(service.findActionType(NS + "Academic").isPresent(), "Academic root missing");
    }

    @Test
    void shouldFindActionTypeByIri() {
        Optional<ActionType> found = service.findActionType(NS + "ClassroomDisruption");
        assertTrue(found.isPresent());
        assertEquals("课堂扰乱", found.get().getLabel());
    }

    @Test
    void shouldReturnEmptyForUnknownIri() {
        Optional<ActionType> found = service.findActionType(NS + "NonExistentType");
        assertFalse(found.isPresent());
    }

    @Test
    void shouldHaveParentChildHierarchy() {
        ActionType behavioral = service.findActionType(NS + "Behavioral").orElseThrow();
        ActionType disruption = service.findActionType(NS + "ClassroomDisruption").orElseThrow();

        assertTrue(disruption.hasParent());
        assertEquals(behavioral, disruption.getParentConcept());
    }

    @Test
    void shouldFindChildrenOfRoot() {
        ActionType behavioral = service.findActionType(NS + "Behavioral").orElseThrow();
        List<ActionType> children = service.findChildren(behavioral);
        assertFalse(children.isEmpty(), "Behavioral should have children");
        assertTrue(children.stream().anyMatch(c -> c.getIri().contains("ClassroomDisruption")));
    }

    @Test
    void shouldGetOrCreateNewActionType() {
        ActionType created = service.getOrCreateActionType("TestType", "behavioral");
        assertNotNull(created);
        assertTrue(created.getIri().contains("TestType"));

        // Should return existing one on second call
        ActionType cached = service.getOrCreateActionType("TestType", "behavioral");
        assertSame(created, cached);
    }

    @Test
    void shouldHaveCorrectCategories() {
        ActionType behavioral = service.findActionType(NS + "Behavioral").orElseThrow();
        ActionType classroom = service.findActionType(NS + "ClassroomDisruption").orElseThrow();
        ActionType academic = service.findActionType(NS + "Academic").orElseThrow();
        ActionType cheating = service.findActionType(NS + "Cheating").orElseThrow();

        assertEquals("behavioral", behavioral.getCategory());
        assertEquals("behavioral", classroom.getCategory());
        assertEquals("academic", academic.getCategory());
        assertEquals("academic", cheating.getCategory());
    }

    @Test
    void shouldRegisterNewTypeDynamically() {
        ActionType newType = new ActionType(NS + "NewType", "新类型", null, "social");
        service.register(newType);

        assertTrue(service.findActionType(NS + "NewType").isPresent());
        assertEquals("新类型", service.findActionType(NS + "NewType").get().getLabel());
    }

    @Test
    void allActionTypesShouldHaveNonEmptyLabels() {
        for (ActionType t : service.getAllActionTypes()) {
            assertNotNull(t.getLabel(), "Label should not be null for " + t.getIri());
            assertFalse(t.getLabel().isEmpty(), "Label should not be empty for " + t.getIri());
        }
    }

    @Test
    void childrenOfAcademicShouldIncludeCheating() {
        ActionType academic = service.findActionType(NS + "Academic").orElseThrow();
        List<ActionType> children = service.findChildren(academic);
        assertTrue(children.stream().anyMatch(c -> c.getIri().contains("Cheating")),
                "Academic children should include Cheating");
    }
}
