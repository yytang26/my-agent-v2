package com.agent.template;

import java.util.Collections;
import java.util.Map;

public class PromptTemplate {

    private String name;
    private String description;
    private String template;
    private Map<String, String> variables;

    public PromptTemplate() {
    }

    public PromptTemplate(String name, String description, String template, Map<String, String> variables) {
        this.name = name;
        this.description = description;
        this.template = template;
        this.variables = variables;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Map<String, String> getVariables() {
        return variables != null ? variables : Collections.emptyMap();
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }

    public String render(Map<String, String> values) {
        return new TemplateRenderer().render(template, values, variables);
    }
}
