package com.ontoevolve.graphstore.mapper;

import com.ontoevolve.core.model.*;
import com.ontoevolve.core.kernel.EvolTrace;
import com.ontoevolve.graphstore.node.*;

import java.util.List;

/**
 * Bidirectional mapper between core domain model POJOs and Neo4j @Node entities.
 *
 * <p>Flat conversion — relationships are resolved at the Store level.
 * Score vectors map to named fields (effectiveness/cost/satisfaction) for queryability.
 * Education-specific types (Intervention, ActionEvent) are handled in the education module.
 */
public class ModelMapper {

    public static final int SCORE_DIMENSION = 3;
    public static final int IDX_EFFECTIVENESS = 0;
    public static final int IDX_COST = 1;
    public static final int IDX_SATISFACTION = 2;

    // ──────────────────────────────────────────────
    // Concept ↔ ActionTypeNode
    // ──────────────────────────────────────────────

    public static ActionTypeNode conceptToNode(Concept concept) {
        if (concept == null) return null;
        ActionTypeNode node = new ActionTypeNode(
                concept.getIri(),
                concept.getLabel(),
                (String) concept.getProperties().getOrDefault("category", ""),
                (String) concept.getProperties().getOrDefault("comment", "")
        );
        if (concept.hasParent()) {
            node.setParent(conceptToNode(concept.getParentConcept()));
        }
        return node;
    }

    public static Concept nodeToConcept(ActionTypeNode node) {
        if (node == null) return null;
        Concept parent = node.getParent() != null ? nodeToConcept(node.getParent()) : null;
        Concept concept = new Concept(node.getIri(), node.getLabel(), parent);
        concept.getProperties().put("category", node.getCategory() != null ? node.getCategory() : "");
        concept.getProperties().put("comment", node.getComment() != null ? node.getComment() : "");
        return concept;
    }

    // ──────────────────────────────────────────────
    // Decision ↔ InterventionNode (base Decision fields only)
    // ──────────────────────────────────────────────

    public static InterventionNode decisionToNode(Decision decision) {
        if (decision == null) return null;
        InterventionNode node = new InterventionNode(
                decision.getIri(),
                decision.getName(),
                decision.getDescription(),
                null,  // interventionType — set by education module if needed
                false
        );
        node.setCreatedAt(decision.getCreatedAt());
        node.setSteps(decision.getSteps() != null ? List.copyOf(decision.getSteps()) : null);
        return node;
    }

    public static Decision nodeToDecision(InterventionNode node) {
        if (node == null) return null;
        List<String> steps = node.getSteps() != null ? List.copyOf(node.getSteps()) : List.of();
        return new Decision(
                node.getIri(),
                node.getName() != null ? node.getName() : "",
                node.getDescription() != null ? node.getDescription() : "",
                steps
        );
    }

    // ──────────────────────────────────────────────
    // Assignment ↔ AssignmentNode
    // ──────────────────────────────────────────────

    public static AssignmentNode assignmentToNode(Assignment assignment) {
        if (assignment == null) return null;
        ActionTypeNode conceptNode = conceptToNode(assignment.getConcept());
        InterventionNode decisionNode = decisionToNode(assignment.getDecision());

        AssignmentNode node = new AssignmentNode(
                assignment.getIri(),
                assignment.getStatus().name(),
                conceptNode,
                decisionNode
        );
        node.setTrials(assignment.getTrials());
        node.setGeneration(assignment.getGeneration());
        node.setLastUpdated(assignment.getLastUpdated());

        double[] scores = assignment.getScoreVector();
        if (scores.length >= SCORE_DIMENSION) {
            node.setEffectiveness(scores[IDX_EFFECTIVENESS]);
            node.setCost(scores[IDX_COST]);
            node.setSatisfaction(scores[IDX_SATISFACTION]);
        }

        return node;
    }

    // ──────────────────────────────────────────────
    // Execution ↔ ExecutionNode
    // ──────────────────────────────────────────────

    public static ExecutionNode executionToNode(Execution execution) {
        if (execution == null) return null;
        AssignmentNode assignmentNode = assignmentToNode(execution.getAssignment());
        ExecutionNode node = new ExecutionNode(
                execution.getIri(),
                execution.getSubjectId(),
                execution.getExecutorId(),
                assignmentNode
        );
        node.setExecutedAt(execution.getExecutedAt());
        return node;
    }

    // ──────────────────────────────────────────────
    // Feedback ↔ EvaluationNode
    // ──────────────────────────────────────────────

    public static EvaluationNode feedbackToNode(Feedback feedback) {
        if (feedback == null) return null;
        double[] scores = feedback.getScores();
        return new EvaluationNode(
                feedback.getIri(),
                scores.length > IDX_EFFECTIVENESS ? scores[IDX_EFFECTIVENESS] : 0,
                scores.length > IDX_COST ? scores[IDX_COST] : 0,
                scores.length > IDX_SATISFACTION ? scores[IDX_SATISFACTION] : 0,
                executionToNode(feedback.getExecution())
        );
    }

    // ──────────────────────────────────────────────
    // EvolTrace ↔ EvolTraceNode
    // ──────────────────────────────────────────────

    public static EvolTraceNode traceToNode(EvolTrace trace,
                                            AssignmentNode producedAssignment,
                                            InterventionNode producedDecision,
                                            List<AssignmentNode> parents) {
        if (trace == null) return null;
        EvolTraceNode node = new EvolTraceNode(
                trace.getId(),
                trace.getOperationType().name(),
                trace.getContextDescription(),
                producedAssignment,
                producedDecision,
                parents
        );
        node.setTimestamp(trace.getTimestamp());
        return node;
    }
}
