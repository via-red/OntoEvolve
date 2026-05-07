package com.ontoevolve.domain.education.service;

import com.ontoevolve.domain.education.model.ActionType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 教育领域本体管理服务。
 * <p>
 * 启动时从 {@code education.ttl} 自动加载概念层次，构建运行时 ActionType 生态位地图。
 * TTL 是概念定义的唯一真相来源，Java 代码不再硬编码类型列表。
 * 同时保留 {@link #getOrCreateActionType(String, String)} 作为 LLM 动态创建新概念的兜底路径。
 */
@Service
public class EducationOntologyService {

    private static final String NS = "http://ontoevolve/education#";

    private static final Set<String> ROOT_IRIS = Set.of(
            NS + "Behavioral",
            NS + "Academic",
            NS + "Social");

    private final Map<String, ActionType> actionTypes = new LinkedHashMap<>();

    public EducationOntologyService() {
        initActionTypes();
    }

    // ====================================================================
    // TTL 驱动初始化
    // ====================================================================

    private void initActionTypes() {
        Model model = loadOntologyModel();
        if (model.isEmpty()) {
            registerFallbackTypes();
            return;
        }

        // 1. 扫描 TTL 中所有 owl:Class，收集 IRI、标签、父子关系
        Map<String, String> parentIriMap = new HashMap<>();
        Map<String, String> labelMap = new LinkedHashMap<>();

        ResIterator classes = model.listSubjectsWithProperty(RDF.type, OWL.Class);
        while (classes.hasNext()) {
            Resource cls = classes.next();
            String iri = cls.getURI();
            if (iri == null || !iri.startsWith(NS)) continue;

            // rdfs:label 作为中文标签
            Statement labelStmt = cls.getProperty(RDFS.label);
            String label = labelStmt != null && labelStmt.getObject().isLiteral()
                    ? labelStmt.getLiteral().getString()
                    : localName(iri);
            labelMap.put(iri, label);

            // rdfs:subClassOf 记录父类
            Statement subStmt = cls.getProperty(RDFS.subClassOf);
            if (subStmt != null && subStmt.getResource().isURIResource()) {
                String parentIri = subStmt.getResource().getURI();
                if (parentIri.startsWith(NS)) {
                    parentIriMap.put(iri, parentIri);
                }
            }
        }

        // 2. 筛选：只保留三大根类别下的行为概念（排除 Intervention 等无关类型）
        Set<String> validIris = new HashSet<>();
        for (String iri : labelMap.keySet()) {
            if (isActionTypeConcept(iri, parentIriMap)) {
                validIris.add(iri);
            }
        }

        // 3. 按深度排序（父类先创建，避免 final parentConcept 前向引用）
        Map<String, Integer> depthMap = new HashMap<>();
        for (String iri : validIris) {
            computeDepth(iri, parentIriMap, depthMap);
        }
        List<String> sorted = new ArrayList<>(validIris);
        sorted.sort(Comparator.comparingInt(depthMap::get));

        // 4. 按序创建并注册
        Map<String, ActionType> created = new HashMap<>();
        for (String iri : sorted) {
            String parentIri = parentIriMap.get(iri);
            ActionType parent = parentIri != null ? created.get(parentIri) : null;
            String category = deriveCategory(iri, parentIriMap);
            ActionType type = new ActionType(iri, labelMap.get(iri), parent, category);
            created.put(iri, type);
            register(type);
        }
    }

    private static Model loadOntologyModel() {
        Model model = ModelFactory.createDefaultModel();
        try {
            var input = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("ontology/education.ttl");
            if (input != null) {
                model.read(input, null, "TURTLE");
            }
        } catch (Exception ignored) {
        }
        return model;
    }

    private static boolean isActionTypeConcept(String iri, Map<String, String> parentIriMap) {
        if (ROOT_IRIS.contains(iri)) return true;
        String parent = parentIriMap.get(iri);
        while (parent != null) {
            if (ROOT_IRIS.contains(parent)) return true;
            parent = parentIriMap.get(parent);
        }
        return false;
    }

    private static int computeDepth(String iri, Map<String, String> parentIriMap, Map<String, Integer> depthMap) {
        if (depthMap.containsKey(iri)) return depthMap.get(iri);
        String parent = parentIriMap.get(iri);
        int depth = parent != null ? computeDepth(parent, parentIriMap, depthMap) + 1 : 0;
        depthMap.put(iri, depth);
        return depth;
    }

    private static String deriveCategory(String iri, Map<String, String> parentIriMap) {
        String current = iri;
        while (current != null) {
            if (ROOT_IRIS.contains(current)) {
                return localName(current).toLowerCase();
            }
            current = parentIriMap.get(current);
        }
        return "behavioral";
    }

    private static String localName(String iri) {
        return iri.substring(iri.lastIndexOf('#') + 1);
    }

    /**
     * 保底注册：TTL 不可用时注册最小必要类型集，确保基本功能不中断。
     */
    private void registerFallbackTypes() {
        ActionType behavioral = new ActionType(NS + "Behavioral", "行为问题", null, "behavioral");
        ActionType academic = new ActionType(NS + "Academic", "学业问题", null, "academic");
        register(behavioral);
        register(academic);
        register(new ActionType(NS + "ClassroomDisruption", "课堂扰乱", behavioral, "behavioral"));
        register(new ActionType(NS + "PeerConflict", "同学冲突", behavioral, "behavioral"));
        register(new ActionType(NS + "Noncompliance", "不服从管理", behavioral, "behavioral"));
        register(new ActionType(NS + "HomeworkMissing", "作业不交", academic, "academic"));
        register(new ActionType(NS + "Cheating", "考试作弊", academic, "academic"));
    }

    // ====================================================================
    // 公共 API
    // ====================================================================

    public void register(ActionType type) {
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
        String iri = NS + label.replaceAll("\\s+", "");
        return actionTypes.computeIfAbsent(iri, k -> {
            ActionType parent = actionTypes.values().stream()
                    .filter(a -> category.equals(a.getCategory()) && !a.hasParent())
                    .findFirst().orElse(null);
            return new ActionType(iri, label, parent, category);
        });
    }
}
