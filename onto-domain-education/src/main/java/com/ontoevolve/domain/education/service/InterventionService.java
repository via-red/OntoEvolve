package com.ontoevolve.domain.education.service;

import com.ontoevolve.core.kernel.EvolutionEngine;
import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.spi.Classifier;
import com.ontoevolve.core.spi.Matcher;
import com.ontoevolve.domain.education.model.ActionEvent;
import com.ontoevolve.domain.education.model.ActionType;
import com.ontoevolve.domain.education.model.Intervention;
import com.ontoevolve.domain.education.model.Evaluation;
import com.ontoevolve.infra.metrics.MetricsCollector;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 教育干预领域编排服务。
 * <p>
 * 协调 Classifier → Matcher → Evolution 三大环节。
 * 这是领域层与框架核心之间的桥梁。
 */
@Service
public class InterventionService {

    private final EducationOntologyService ontologyService;
    private final EvolutionEngine evolutionEngine;
    private final Matcher<ActionType, Intervention, Assignment> matcher;
    private final MetricsCollector metrics;
    private final Classifier<ActionEvent, ActionType> classifier;

    public InterventionService(EducationOntologyService ontologyService,
                               EvolutionEngine evolutionEngine,
                               Matcher<?, ?, ?> matcher,
                               MetricsCollector metrics,
                               Classifier<ActionEvent, ActionType> classifier) {
        this.ontologyService = ontologyService;
        this.evolutionEngine = evolutionEngine;
        this.matcher = (Matcher<ActionType, Intervention, Assignment>) matcher;
        this.metrics = metrics;
        this.classifier = classifier;
    }

    /**
     * 处理一条行为事件：
     * 1. 分类找到生态位（LLM 语义分类）
     * 2. 从生态位匹配最优方案
     * 3. 返回建议的干预方案
     */
    public Intervention processEvent(ActionEvent event) {
        // 1. LLM 分类：基于自然语言描述确定行为类型
        ActionType actionType = classifier.classify(event);

        // 2. 沿概念层次递归匹配，子概念无匹配时回退父概念
        return findMatchHierarchy(actionType, event);
    }

    /** 递归查找：从 actionType 开始匹配，若无匹配则回退父概念 */
    private Intervention findMatchHierarchy(ActionType actionType, ActionEvent event) {
        if (actionType == null) return null;

        var pop = evolutionEngine.getOrCreatePopulation(actionType, 12);
        var context = new Matcher.Context(event.getStudentId(),
                Map.of("severity", event.getSeverity()));
        Assignment matched = matcher.match(actionType, context, pop.getActiveMembers());

        if (matched != null) {
            return (Intervention) matched.getDecision();
        }

        // 回退到父概念
        Concept parent = actionType.getParentConcept();
        if (parent instanceof ActionType parentType) {
            return findMatchHierarchy(parentType, event);
        }

        return null;
    }

    /**
     * 提交反馈评价。
     */
    public void submitEvaluation(com.ontoevolve.core.model.Execution execution,
                                 double effectiveness, double cost, double satisfaction) {
        Evaluation evaluation = new Evaluation(
                "eval:" + UUID.randomUUID(),
                execution,
                effectiveness, cost, satisfaction
        );

        // 更新 Assignment 统计量
        execution.getAssignment().updateScore(evaluation.getScores());
        metrics.recordFeedback();

        // 通知进化引擎
        evolutionEngine.recordFeedback(execution.getAssignment().getConcept());
    }

    /**
     * 手动触发一个生态位的完整进化代。
     */
    public void evolveNiche(String conceptIri) {
        ontologyService.findActionType(conceptIri)
                .ifPresent(evolutionEngine::runFullEvolution);
        metrics.recordEvolutionRun();
    }

    public MetricsCollector getMetrics() { return metrics; }
    public EvolutionEngine getEvolutionEngine() { return evolutionEngine; }
}
