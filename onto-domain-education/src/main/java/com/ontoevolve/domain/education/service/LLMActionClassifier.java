package com.ontoevolve.domain.education.service;

import com.ontoevolve.core.spi.Classifier;
import com.ontoevolve.domain.education.model.ActionEvent;
import com.ontoevolve.domain.education.model.ActionType;
import com.ontoevolve.infra.llm.LLMClient;
import com.ontoevolve.infra.llm.LLMResponse;
import com.ontoevolve.infra.llm.PromptTemplateService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LLM 驱动的行为分类器。
 * <p>
 * 使用 PromptTemplateService 加载外部 prompt 模板。
 */
@Component
public class LLMActionClassifier implements Classifier<ActionEvent, ActionType> {

    private final LLMClient llmClient;
    private final EducationOntologyService ontologyService;
    private final PromptTemplateService promptService;

    public LLMActionClassifier(LLMClient llmClient,
                               EducationOntologyService ontologyService,
                               PromptTemplateService promptService) {
        this.llmClient = llmClient;
        this.ontologyService = ontologyService;
        this.promptService = promptService;
    }

    public ActionType classify(ActionEvent event) {
        String typeList = String.join("\n",
                ontologyService.getAllActionTypes().stream()
                        .map(t -> "- " + t.getLabel() + " (" + t.getIri() + ")")
                        .toList());

        String prompt = promptService.render("classifier", Map.of(
                "description", event.getBehaviorDescription(),
                "severity", event.getSeverity(),
                "location", event.getLocation(),
                "typeList", typeList
        ));

        LLMResponse response = llmClient.generate(prompt);
        return resolveActionType(response.content(), event);
    }

    private ActionType resolveActionType(String llmResponse, ActionEvent event) {
        if (llmResponse == null || llmResponse.isBlank()) {
            return fallbackClassify(event);
        }

        for (ActionType type : ontologyService.getAllActionTypes()) {
            if (llmResponse.contains(type.getLabel())
                    || type.getLabel().contains(llmResponse.trim())) {
                return type;
            }
        }

        return fallbackClassify(event);
    }

    private ActionType fallbackClassify(ActionEvent event) {
        String desc = event.getBehaviorDescription().toLowerCase();
        String category = desc.contains("作业") || desc.contains("考试")
                ? "academic" : "behavioral";
        return ontologyService.getOrCreateActionType(
                event.getBehaviorDescription(), category);
    }

    @Override
    public boolean supportsUnknown() {
        return true;
    }
}
