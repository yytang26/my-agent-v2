package com.agent.mcp;

import com.agent.tool.ToolDefinition;
import com.agent.tool.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class McpToolAdapter {

    public ToolDefinition toToolDefinition(McpClient.McpToolInfo info) {
        return new ToolDefinition(info.name(), info.description(), info.inputSchema());
    }

    public ToolResult execute(McpClient client, String toolName, String toolUseId, Map<String, Object> args) {
        try {
            String result = client.callTool(toolName, args);
            return ToolResult.success(toolUseId, result);
        } catch (Exception e) {
            return ToolResult.error(toolUseId, "MCP tool execution failed: " + e.getMessage());
        }
    }
}
