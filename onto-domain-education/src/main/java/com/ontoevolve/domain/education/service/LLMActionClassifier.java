package com.ontoevolve.domain.education.service;

import com.ontoevolve.domain.education.model.ActionEvent;
import com.ontoevolve.domain.education.model.ActionType;
import com.ontoevolve.infra.llm.LLMClient;
import org.springframework.stereotype.Component;

/**
 * LLM 驱动的行为分类器。
 * <p>
 * 将自然语言描述的学生行为事件分类到本体定义的 ActionType 生态位。
 * 利用 Spring AI ChatClient（可切换底层模型）进行语义理解。
 */
@Component
public class LLMActionClassifier {

    private final LLMClient llmClient;
    private final EducationOntologyService ontologyService;

    public LLMActionClassifier(LLMClient llmClient,
                               EducationOntologyService ontologyService) {
        this.llmClient = llmClient;
        this.ontologyService = ontologyService;
    }

    /**
     * 将行为事件分类为对应的 ActionType。
     */
    public ActionType classify(ActionEvent event) {
        String prompt = buildPrompt(event);
        String llmResponse = llmClient.generate(prompt);
        return resolveActionType(llmResponse, event);
    }

    private String buildPrompt(ActionEvent event) {
        String typeList = String.join("\n",
                ontologyService.getAllActionTypes().stream()
                        .map(t -> "- " + t.getLabel() + " (" + t.getIri() + ")")
                        .toList());

        return String.format("""
                将以下学生行为分类到最合适的类别中。

                行为描述: %s
                严重程度: %s
                地点: %s

                可选类别:
                %s

                请只返回最匹配类别的名称，不要额外解释。
                """,
                event.getBehaviorDescription(),
                event.getSeverity(),
                event.getLocation(),
                typeList);
    }

    private ActionType resolveActionType(String llmResponse, ActionEvent event) {
        // 精确匹配
        for (ActionType type : ontologyService.getAllActionTypes()) {
            if (llmResponse.contains(type.getLabel())
                    || type.getLabel().contains(llmResponse.trim())) {
                return type;
            }
        }

        // 未匹配时：按关键词猜 + 自动注册
        String desc = event.getBehaviorDescription().toLowerCase();
        String category = desc.contains("作业") || desc.contains("考试")
                ? "academic" : "behavioral";
        return ontologyService.getOrCreateActionType(
                event.getBehaviorDescription(), category);
    }
}
