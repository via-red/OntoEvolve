package com.ontoevolve.saas.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DomainConfig {

    private String id;
    private String name;
    private String description;
    private String namespace;
    private DomainStatus status = DomainStatus.DRAFT;
    private LlmConfig llm = new LlmConfig();
    private StoreConfig store = new StoreConfig();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }

    public DomainStatus getStatus() { return status; }
    public void setStatus(DomainStatus status) { this.status = status; }

    public LlmConfig getLlm() { return llm; }
    public void setLlm(LlmConfig llm) { this.llm = llm; }

    public StoreConfig getStore() { return store; }
    public void setStore(StoreConfig store) { this.store = store; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LlmConfig {
        private String provider = "openai";
        private String model = "deepseek-v4-flash";

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }
}
