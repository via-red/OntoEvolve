package com.ontoevolve.domain.education.service;

import com.ontoevolve.core.kernel.EvolutionEngine;
import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.model.Concept;
import com.ontoevolve.core.model.Execution;
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

        // 2. 确保种群已播种初始方案
        seedPopulationIfEmpty(actionType);

        // 3. 沿概念层次递归匹配，子概念无匹配时回退父概念
        return findMatchHierarchy(actionType, event);
    }

    /** 如果种群为空，播种初始干预方案 */
    private void seedPopulationIfEmpty(ActionType actionType) {
        var pop = evolutionEngine.getOrCreatePopulation(actionType, 12);
        if (!pop.getActiveMembers().isEmpty()) return;

        // 根据分类类别选择合适的初始干预方案
        List<Intervention> seeds = createSeedInterventions(actionType);
        for (Intervention intervention : seeds) {
            Assignment assignment = new Assignment(
                    "seed:" + UUID.randomUUID(),
                    intervention,
                    actionType,
                    3 // effectiveness, cost, satisfaction
            );
            assignment.setStatus(Assignment.Status.ACTIVE);
            assignment.setGeneration(0);
            pop.addMember(assignment);
        }
    }

    /** 创建初始种子干预方案 */
    private List<Intervention> createSeedInterventions(ActionType actionType) {
        List<Intervention> seeds = new ArrayList<>();
        String cat = actionType.getCategory();

        // 所有类别通用的基础方案
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
     * Submit evaluation with an Assignment (creates a proper Execution record).
     */
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

        assignment.updateScore(evaluation.getScores());
        evolutionEngine.recordFeedback(assignment.getConcept());
        metrics.recordFeedback();
    }

    /**
     * Legacy: submit evaluation by intervention IRI.
     * Searches all populations for the matching assignment.
     */
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
