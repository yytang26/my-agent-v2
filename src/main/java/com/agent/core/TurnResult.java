package com.agent.core;

import com.agent.tool.ToolResult;

import java.util.List;
import java.util.Map;

public class TurnResult {

    private AgentState state;
    private String assistantMessage;
    private List<ToolCall> toolCalls;
    private List<ToolResult> toolResults;
    private int iteration;

    public TurnResult(AgentState state, String assistantMessage, List<ToolCall> toolCalls,
                      List<ToolResult> toolResults, int iteration) {
        this.state = state;
        this.assistantMessage = assistantMessage;
        this.toolCalls = toolCalls;
        this.toolResults = toolResults;
        this.iteration = iteration;
    }

    public AgentState getState() {
        return state;
    }

    public void setState(AgentState state) {
        this.state = state;
    }

    public String getAssistantMessage() {
        return assistantMessage;
    }

    public void setAssistantMessage(String assistantMessage) {
        this.assistantMessage = assistantMessage;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public List<ToolResult> getToolResults() {
        return toolResults;
    }

    public void setToolResults(List<ToolResult> toolResults) {
        this.toolResults = toolResults;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public record ToolCall(String id, String name, Map<String, Object> arguments) {
    }
}
