package com.agent.config;

/**
 * Agent 配置 POJO，包含所有可配置项。
 * 全部字段可为 null，便于分层合并时判断某一层是否提供了该配置。
 */
public class AgentConfig {

    private String llmProvider;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private String apiKey;
    private String baseUrl;

    public AgentConfig() {
    }

    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * 用 other 的非空值覆盖当前对象的对应字段。
     *
     * @param other 来源配置
     * @return this，支持链式调用
     */
    public AgentConfig merge(AgentConfig other) {
        if (other == null) {
            return this;
        }
        if (other.llmProvider != null) {
            this.llmProvider = other.llmProvider;
        }
        if (other.model != null) {
            this.model = other.model;
        }
        if (other.temperature != null) {
            this.temperature = other.temperature;
        }
        if (other.maxTokens != null) {
            this.maxTokens = other.maxTokens;
        }
        if (other.apiKey != null) {
            this.apiKey = other.apiKey;
        }
        if (other.baseUrl != null) {
            this.baseUrl = other.baseUrl;
        }
        return this;
    }

    @Override
    public String toString() {
        return "AgentConfig{" +
                "llmProvider='" + llmProvider + '\'' +
                ", model='" + model + '\'' +
                ", temperature=" + temperature +
                ", maxTokens=" + maxTokens +
                ", apiKey='" + (apiKey != null ? "***" : null) + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                '}';
    }
}
