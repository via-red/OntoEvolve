package com.ontoevolve.domain.education.service;

import com.ontoevolve.core.kernel.EvolutionEngine;
import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Execution;
import com.ontoevolve.core.model.Feedback;
import com.ontoevolve.core.spi.*;
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
 * 支持 Environment 自动评估和 CreditAssigner 延迟信用分配。
 */
@Service
public class InterventionService {

    private final EducationOntologyService ontologyService;
    private final EvolutionEngine evolutionEngine;
    private final Matcher<ActionType, Intervention, Assignment> matcher;
    private final MetricsCollector metrics;
    private final Classifier<ActionEvent, ActionType> classifier;
    private final Environment environment;
    private final CreditAssigner creditAssigner;
    private final List<Execution> executionHistory = Collections.synchronizedList(new ArrayList<>());

    public InterventionService(EducationOntologyService ontologyService,
                               EvolutionEngine evolutionEngine,
                               Matcher<?, ?, ?> matcher,
                               MetricsCollector metrics,
                               Classifier<ActionEvent, ActionType> classifier,
                               Environment environment,
                               CreditAssigner creditAssigner) {
        this.ontologyService = ontologyService;
        this.evolutionEngine = evolutionEngine;
        this.matcher = (Matcher<ActionType, Intervention, Assignment>) matcher;
        this.metrics = metrics;
        this.classifier = classifier;
        this.environment = environment;
        this.creditAssigner = creditAssigner;
    }

    /**
     * 处理一条行为事件：分类 → 匹配 → 返回建议方案。
     */
    public Intervention processEvent(ActionEvent event) {
        ActionType actionType = classifier.classify(event);
        seedPopulationIfEmpty(actionType);
        return findMatchHierarchy(actionType, event);
    }

    private void seedPopulationIfEmpty(ActionType actionType) {
        var pop = evolutionEngine.getOrCreatePopulation(actionType, 12);
        if (!pop.getActiveMembers().isEmpty()) return;

        List<Intervention> seeds = createSeedInterventions(actionType);
        for (Intervention intervention : seeds) {
            Assignment assignment = new Assignment(
                    "seed:" + UUID.randomUUID(),
                    intervention,
                    actionType,
                    3
            );
            assignment.setStatus(Assignment.Status.ACTIVE);
            assignment.setGeneration(0);
            pop.addMember(assignment);
        }
    }

    private List<Intervention> createSeedInterventions(ActionType actionType) {
        List<Intervention> seeds = new ArrayList<>();
        String cat = actionType.getCategory();

        seeds.add(new Intervention(
                "seed:talk", "一对一谈话",
                "与学生进行一对一谈话，了解情况并引导改进",
                List.of("了解事情经过", "倾听学生解释", "指出行为的不当之处", "共同约定改进目标"),
                "talk", false));

        seeds.add(new Intervention(
                "seed:notice", "通知家长",
                "通知家长学生在校表现，协同教育",
                List.of("电话联系家长", "说明学生在校表现", "建议家庭教育配合", "约定后续沟通频率"),
                "notice", true));

        if ("academic".equals(cat)) {
            seeds.add(new Intervention(
                    "seed:tutoring", "课后补习计划",
                    "针对学业问题制定课后补习计划",
                    List.of("摸底薄弱科目", "制定补习计划", "安排同学结对帮扶", "每周检查学习进展"),
                    "academic", false));
            seeds.add(new Intervention(
                    "seed:study-habit", "学习习惯指导",
                    "帮助学生改进学习方法",
                    List.of("分析学习方法问题", "制定作息时间表", "教授笔记和复习技巧", "跟踪执行情况"),
                    "academic", false));
        } else if ("behavioral".equals(cat)) {
            seeds.add(new Intervention(
                    "seed:reward", "行为积分奖励",
                    "设立正向激励以强化良好行为",
                    List.of("设立行为改善目标", "每日记录进步情况", "达到目标给予班级表彰", "累积积分兑换奖励"),
                    "reward", false));
            seeds.add(new Intervention(
                    "seed:counseling", "心理辅导转介",
                    "转介学校心理辅导老师进行专业干预",
                    List.of("与心理老师沟通情况", "安排定期心理咨询", "关注学生情绪变化", "与家长保持沟通"),
                    "counseling", true));
        } else {
            seeds.add(new Intervention(
                    "seed:activity", "集体活动引导",
                    "通过课外活动引导行为改善",
                    List.of("了解学生兴趣方向", "安排参与班级服务岗位", "鼓励展示特长", "定期给予正向反馈"),
                    "activity", false));
        }

        return seeds;
    }

    private Intervention findMatchHierarchy(ActionType actionType, ActionEvent event) {
        if (actionType == null) return null;

        var pop = evolutionEngine.getOrCreatePopulation(actionType, 12);
        var context = new Matcher.Context(event.getStudentId(),
                Map.of("severity", event.getSeverity()));
        Assignment matched = matcher.match(actionType, context, pop.getActiveMembers());

        if (matched != null) {
            return (Intervention) matched.getDecision();
        }

        Concept parent = actionType.getParentConcept();
        if (parent instanceof ActionType parentType) {
            return findMatchHierarchy(parentType, event);
        }

        return null;
    }

    /** 提交人工评估。 */
    public void submitEvaluation(Assignment assignment, String studentId,
                                 double effectiveness, double cost, double satisfaction) {
        Execution execution = new Execution(
                "exec:" + UUID.randomUUID(),
                assignment,
                studentId,
                "system",
                Map.of()
        );

        Evaluation evaluation = new Evaluation(
                "eval:" + UUID.randomUUID(),
                execution,
                effectiveness, cost, satisfaction
        );

        applyFeedback(assignment, execution, evaluation);
    }

    /** 自动评估（使用 Environment）。 */
    public void submitAutomatedEvaluation(Assignment assignment, String studentId) {
        if (environment == null || !environment.isAutomated()) return;

        Execution execution = new Execution(
                "exec:" + UUID.randomUUID(),
                assignment,
                studentId,
                "auto",
                Map.of()
        );

        Feedback feedback = environment.evaluate(assignment, execution);
        applyFeedback(assignment, execution, feedback);
    }

    private void applyFeedback(Assignment assignment, Execution execution, Feedback feedback) {
        assignment.updateScore(feedback.getScores());
        evolutionEngine.recordFeedback(assignment.getConcept());
        metrics.recordFeedback();
        executionHistory.add(execution);

        // 信用分配：将延迟反馈传播到历史执行
        distributeCredit(feedback, assignment.getConcept());
    }

    private void distributeCredit(Feedback feedback, Concept concept) {
        if (creditAssigner == null || executionHistory.isEmpty()) return;

        List<Execution> relevant = executionHistory.stream()
                .filter(e -> e.getAssignment().getConcept().getIri().equals(concept.getIri()))
                .toList();

        if (!relevant.isEmpty()) {
            List<Feedback> distributed = creditAssigner.distribute(feedback, relevant);
            for (Feedback dfb : distributed) {
                if (!dfb.getIri().equals(feedback.getIri())) {
                    Assignment target = dfb.getExecution().getAssignment();
                    target.updateScore(dfb.getScores());
                }
            }
        }
    }

    /** 按干预 IRI 提交评估（搜索所有种群）。 */
    public void submitEvaluationByIntervention(String interventionIri, String studentId,
                                                double effectiveness, double cost, double satisfaction) {
        Assignment found = findAssignmentByIntervention(interventionIri);
        if (found != null) {
            submitEvaluation(found, studentId, effectiveness, cost, satisfaction);
        } else {
            metrics.recordFeedback();
        }
    }

    private Assignment findAssignmentByIntervention(String interventionIri) {
        return evolutionEngine.getPopulations().values().stream()
                .flatMap(pop -> pop.getActiveMembers().stream())
                .filter(a -> a.getDecision().getIri().equals(interventionIri))
                .findFirst().orElse(null);
    }

    /** 手动触发一个生态位的完整进化代。 */
    public void evolveNiche(String conceptIri) {
        ontologyService.findActionType(conceptIri)
                .ifPresent(evolutionEngine::runFullEvolution);
        metrics.recordEvolutionRun();
    }

    public MetricsCollector getMetrics() { return metrics; }
    public EvolutionEngine getEvolutionEngine() { return evolutionEngine; }
}
