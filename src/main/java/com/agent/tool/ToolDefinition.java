package com.agent.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ToolDefinition {

    private final String name;
    private final String description;
    private final Map<String, Object> inputSchema;

    public ToolDefinition(String name, String description, Map<String, Object> inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("input_schema", inputSchema);
        return map;
    }

    @Override
    public String toString() {
        return "ToolDefinition{name='" + name + "', description='" + description + "'}";
    }
}
