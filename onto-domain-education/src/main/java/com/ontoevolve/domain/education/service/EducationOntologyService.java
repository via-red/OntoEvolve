package com.ontoevolve.domain.education.service;

import com.ontoevolve.core.model.Concept;
import com.ontoevolve.domain.education.model.ActionType;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 教育领域本体管理服务。
 * <p>
 * 管理教育领域的 Concept 层次结构、干预方案目录。
 * 生产环境中应由 OWL 本体文件驱动，此处为内存示例。
 */
@Service
public class EducationOntologyService {

    private final Map<String, ActionType> actionTypes = new HashMap<>();

    public EducationOntologyService() {
        initActionTypes();
    }

    private void initActionTypes() {
        // 一级行为分类
        ActionType behavioral = new ActionType(
                "http://ontoevolve/education#Behavioral",
                "行为问题", null, "behavioral");

        ActionType academic = new ActionType(
                "http://ontoevolve/education#Academic",
                "学业问题", null, "academic");

        // 二级行为分类
        new ActionType("http://ontoevolve/education#ClassroomDisruption",
                "课堂扰乱", behavioral, "behavioral");
        new ActionType("http://ontoevolve/education#PeerConflict",
                "同学冲突", behavioral, "behavioral");
        new ActionType("http://ontoevolve/education#Noncompliance",
                "不服从管理", behavioral, "behavioral");
        new ActionType("http://ontoevolve/education#HomeworkMissing",
                "作业不交", academic, "academic");
        new ActionType("http://ontoevolve/education#Cheating",
                "考试作弊", academic, "academic");
        new ActionType("http://ontoevolve/education#Cyberbullying",
                "网络欺凌", behavioral, "behavioral");

        // 注册
        register(behavioral);
        register(academic);
    }

    private void register(ActionType type) {
        actionTypes.put(type.getIri(), type);
    }

    public Optional<ActionType> findActionType(String iri) {
        return Optional.ofNullable(actionTypes.get(iri));
    }

    public List<ActionType> getAllActionTypes() {
        return List.copyOf(actionTypes.values());
    }

    public List<ActionType> findChildren(ActionType parent) {
        return actionTypes.values().stream()
                .filter(a -> a.getParentConcept() != null
                        && a.getParentConcept().equals(parent))
                .toList();
    }

    public ActionType getOrCreateActionType(String label, String category) {
        String iri = "http://ontoevolve/education#" + label.replaceAll("\\s+", "");
        return actionTypes.computeIfAbsent(iri, k -> {
            ActionType parent = actionTypes.values().stream()
                    .filter(a -> a.getCategory().equals(category) && !a.hasParent())
                    .findFirst().orElse(null);
            return new ActionType(iri, label, parent, category);
        });
    }
}
