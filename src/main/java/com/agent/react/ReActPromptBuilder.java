package com.agent.react;

import com.agent.tool.ToolDefinition;
import com.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ReActPromptBuilder {

    private final ToolRegistry toolRegistry;

    public ReActPromptBuilder(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public String buildSystemPrompt() {
        String toolsList = formatToolsList();
        return """
                你是一个有帮助的 AI 助手，使用 ReAct（Reasoning + Acting）框架来解决问题。

                对于每一步，你需要：
                1. Thought: 分析当前情况，思考下一步该做什么
                2. Action: 选择一个工具执行（格式: tool_name(param1="value1", param2="value2")）
                3. Observation: 观察工具执行结果

                当你有了最终答案时：
                Thought: 我已经有了足够的信息来回答
                Answer: [你的最终回答]

                可用工具:
                """ + toolsList;
    }

    public String formatToolsList() {
        List<ToolDefinition> definitions = toolRegistry.getAllToolDefinitions();
        if (definitions.isEmpty()) {
            return "  (无可用工具)\n";
        }

        StringBuilder sb = new StringBuilder();
        for (ToolDefinition def : definitions) {
            sb.append(String.format("  - %s: %s%n", def.getName(), def.getDescription()));
            Map<String, Object> schema = def.getInputSchema();
            if (schema != null && schema.containsKey("properties")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> props = (Map<String, Object>) schema.get("properties");
                if (props != null && !props.isEmpty()) {
                    sb.append("    参数:\n");
                    for (Map.Entry<String, Object> entry : props.entrySet()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> propInfo = (Map<String, Object>) entry.getValue();
                        String type = propInfo.containsKey("type") ? propInfo.get("type").toString() : "string";
                        String desc = propInfo.containsKey("description") ? propInfo.get("description").toString() : "";
                        sb.append(String.format("      %s (%s)%s%n",
                                entry.getKey(),
                                type,
                                desc.isEmpty() ? "" : " - " + desc));
                    }
                }
            }
        }
        return sb.toString();
    }
}
