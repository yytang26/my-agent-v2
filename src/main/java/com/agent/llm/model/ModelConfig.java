package com.agent.llm.model;

public class ModelConfig {

    private String name;
    private double temperature;
    private int maxTokens;

    public ModelConfig(String name, double temperature, int maxTokens) {
        this.name = name;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private double temperature = 0.7;
        private int maxTokens = 1024;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public ModelConfig build() {
            return new ModelConfig(name, temperature, maxTokens);
        }
    }
}
