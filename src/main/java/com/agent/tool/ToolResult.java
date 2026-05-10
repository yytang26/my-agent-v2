package com.agent.tool;

public class ToolResult {

    private final String toolUseId;
    private final String content;
    private final boolean isError;

    private ToolResult(String toolUseId, String content, boolean isError) {
        this.toolUseId = toolUseId;
        this.content = content;
        this.isError = isError;
    }

    public static ToolResult success(String toolUseId, String content) {
        return new ToolResult(toolUseId, content, false);
    }

    public static ToolResult error(String toolUseId, String message) {
        return new ToolResult(toolUseId, message, true);
    }

    public String getToolUseId() {
        return toolUseId;
    }

    public String getContent() {
        return content;
    }

    public boolean isError() {
        return isError;
    }

    @Override
    public String toString() {
        return "ToolResult{toolUseId='" + toolUseId + "', isError=" + isError + ", content='" + content + "'}";
    }
}
