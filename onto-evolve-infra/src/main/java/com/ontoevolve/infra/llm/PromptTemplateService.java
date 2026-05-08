package com.ontoevolve.infra.llm;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt 模板服务 — 加载 classpath 下的 .st 模板并渲染。
 * <p>
 * 模板使用 {key} 占位符语法。
 * 使用缓存避免重复读取 classpath 资源。
 */
public class PromptTemplateService {

    private final ResourceLoader resourceLoader;
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    private static final Map<String, String> TEMPLATE_PATHS = Map.of(
            "classifier", "classpath:prompts/classifier.st",
            "variator_generate", "classpath:prompts/variator_generate.st",
            "variator_crossover", "classpath:prompts/variator_crossover.st"
    );

    public PromptTemplateService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /** 渲染指定名称的模板，替换 {key} 占位符。 */
    public String render(String templateName, Map<String, Object> variables) {
        String template = templateCache.computeIfAbsent(templateName, this::loadTemplate);
        String result = template;
        for (var entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}",
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }

    private String loadTemplate(String templateName) {
        String path = TEMPLATE_PATHS.get(templateName);
        if (path == null) {
            throw new IllegalArgumentException("Unknown template: " + templateName);
        }
        try {
            Resource resource = resourceLoader.getResource(path);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load template: " + templateName, e);
        }
    }

    /** 注册自定义模板（用于领域扩展）。 */
    public void registerTemplate(String name, String path) {
        TEMPLATE_PATHS.put(name, path);
        templateCache.remove(name);
    }
}
