package com.ontoevolve.saas.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontoevolve.saas.config.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DomainConfigService {

    private static final Logger log = LoggerFactory.getLogger(DomainConfigService.class);
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private final Path domainsPath;
    private final ObjectMapper objectMapper;
    private final DomainCache cache;

    public DomainConfigService(
            @Value("${onto.saas.domains-path:data/domains}") String domainsPath,
            ObjectMapper objectMapper,
            DomainCache cache) {
        this.domainsPath = Path.of(domainsPath);
        this.objectMapper = objectMapper;
        this.cache = cache;
    }

    @PostConstruct
    public void loadAllDomains() {
        File dir = domainsPath.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Domains directory not found: {}", domainsPath);
            return;
        }

        File[] domainDirs = dir.listFiles(File::isDirectory);
        if (domainDirs == null) return;

        for (File domainDir : domainDirs) {
            try {
                DomainDefinition def = loadDomain(domainDir);
                if (def != null && def.getDomain() != null) {
                    cache.put(def.getId(), def);
                    log.info("Loaded domain: {} ({})", def.getDomain().getName(), def.getId());
                }
            } catch (Exception e) {
                log.error("Failed to load domain from {}", domainDir, e);
            }
        }
        log.info("Loaded {} domains total", cache.getAll().size());
    }

    public DomainDefinition getDomain(String id) {
        return cache.get(id);
    }

    public List<DomainDefinition> getAllDomains() {
        return List.copyOf(cache.getAll().values());
    }

    public List<DomainDefinition> getPublishedDomains() {
        return cache.getAll().values().stream()
                .filter(d -> d.getDomain().getStatus() == DomainStatus.PUBLISHED)
                .collect(Collectors.toList());
    }

    public DomainDefinition createDomain(String id, String name) throws IOException {
        File domainDir = domainsPath.resolve(id).toFile();
        if (!domainDir.mkdirs()) {
            throw new IOException("Domain directory already exists: " + id);
        }

        DomainConfig config = new DomainConfig();
        config.setId(id);
        config.setName(name != null ? name : id);
        config.setNamespace("http://ontoevolve/" + id + "#");

        DomainDefinition def = new DomainDefinition();
        def.setDomain(config);
        def.setConcepts(new ArrayList<>());
        def.setDecisionTypes(new ArrayList<>());
        def.setConceptMappings(new ArrayList<>());
        def.setFeedbackDimensions(new ArrayList<>());
        def.setClassificationRules(new ArrayList<>());
        def.setFieldDefinitions(new ArrayList<>());

        saveDomain(def);
        cache.put(id, def);
        return def;
    }

    public void saveDomain(DomainDefinition def) throws IOException {
        File domainDir = domainsPath.resolve(def.getId()).toFile();
        domainDir.mkdirs();

        writeJson(domainDir, "domain.json", def.getDomain());
        writeJson(domainDir, "ontology.json", Map.of("concepts", def.getConcepts()));
        writeJson(domainDir, "decisions.json", Map.of(
                "decisionTypes", def.getDecisionTypes(),
                "conceptMappings", def.getConceptMappings()));
        writeJson(domainDir, "feedback.json", Map.of("dimensions", def.getFeedbackDimensions()));
        writeJson(domainDir, "rules.json", Map.of("classificationRules", def.getClassificationRules()));
        writeJson(domainDir, "fields.json", Map.of("fieldDefinitions", def.getFieldDefinitions()));
    }

    public void deleteDomain(String id) throws IOException {
        cache.remove(id);
        File domainDir = domainsPath.resolve(id).toFile();
        if (domainDir.exists()) {
            deleteDirectory(domainDir);
        }
    }

    public void reloadDomain(String id) {
        cache.remove(id);
        File domainDir = domainsPath.resolve(id).toFile();
        if (domainDir.exists()) {
            try {
                DomainDefinition def = loadDomain(domainDir);
                if (def != null) cache.put(def.getId(), def);
            } catch (Exception e) {
                log.error("Failed to reload domain {}", id, e);
            }
        }
    }

    // ---- JSON file reading helpers ----

    private DomainDefinition loadDomain(File dir) throws IOException {
        DomainDefinition def = new DomainDefinition();
        def.setDomain(readJson(dir, "domain.json", DomainConfig.class));
        if (def.getDomain() == null) return null;

        resolveEnvVars(def.getDomain().getStore().getConfig());

        def.setConcepts(readNestedList(dir, "ontology.json", "concepts", ConceptConfig.class));
        def.setDecisionTypes(readNestedList(dir, "decisions.json", "decisionTypes", DecisionTypeConfig.class));
        def.setConceptMappings(readNestedList(dir, "decisions.json", "conceptMappings", ConceptMappingConfig.class));
        def.setFeedbackDimensions(readNestedList(dir, "feedback.json", "dimensions", FeedbackDimensionConfig.class));
        def.setClassificationRules(readNestedList(dir, "rules.json", "classificationRules", ClassificationRuleConfig.class));
        def.setFieldDefinitions(readNestedList(dir, "fields.json", "fieldDefinitions", FieldDefinitionConfig.class));

        return def;
    }

    private <T> T readJson(File dir, String fileName, Class<T> clazz) throws IOException {
        File file = new File(dir, fileName);
        if (!file.exists()) return null;
        return objectMapper.readValue(file, clazz);
    }

    private <T> List<T> readNestedList(File dir, String fileName, String fieldName, Class<T> elementClass) throws IOException {
        File file = new File(dir, fileName);
        if (!file.exists()) return new ArrayList<>();

        Map<String, Object> root = objectMapper.readValue(file, new TypeReference<>() {});
        Object raw = root.get(fieldName);
        if (raw == null) return new ArrayList<>();

        List<T> result = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                T converted = objectMapper.convertValue(item, elementClass);
                if (converted != null) result.add(converted);
            }
        }
        return result;
    }

    private void writeJson(File dir, String fileName, Object value) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(dir, fileName), value);
    }

    @SuppressWarnings("unchecked")
    private void resolveEnvVars(Map<String, Object> config) {
        if (config == null) return;
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getValue() instanceof String str) {
                config.put(entry.getKey(), resolveEnv(str));
            }
        }
    }

    static String resolveEnv(String value) {
        if (value == null || !value.contains("${")) return value;
        Matcher m = ENV_VAR_PATTERN.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String envVar = m.group(1);
            String replacement = System.getenv(envVar);
            if (replacement == null) {
                replacement = System.getProperty(envVar);
            }
            if (replacement == null) {
                replacement = m.group(0); // leave unresolved
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private void deleteDirectory(File dir) throws IOException {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) deleteDirectory(f);
            }
        }
        Files.deleteIfExists(dir.toPath());
    }
}
