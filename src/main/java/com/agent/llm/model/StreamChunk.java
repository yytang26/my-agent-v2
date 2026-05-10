package com.agent.llm.model;

import java.util.Map;

public class StreamChunk {

    private StreamEventType type;
    private String text;
    private String toolUseId;
    private String toolName;
    private Map<String, Object> toolInput;
    private int index;

    public StreamChunk() {
    }

    public StreamChunk(StreamEventType type) {
        this.type = type;
    }

    public static StreamChunk textDelta(String text) {
        StreamChunk chunk = new StreamChunk(StreamEventType.CONTENT_BLOCK_DELTA);
        chunk.setText(text);
        return chunk;
    }

    public static StreamChunk toolUseStart(String id, String name) {
        StreamChunk chunk = new StreamChunk(StreamEventType.CONTENT_BLOCK_START);
        chunk.setToolUseId(id);
        chunk.setToolName(name);
        return chunk;
    }

    public static StreamChunk inputJsonDelta(String json) {
        StreamChunk chunk = new StreamChunk(StreamEventType.CONTENT_BLOCK_DELTA);
        chunk.setText(json);
        return chunk;
    }

    public StreamEventType getType() {
        return type;
    }

    public void setType(StreamEventType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getToolUseId() {
        return toolUseId;
    }

    public void setToolUseId(String toolUseId) {
        this.toolUseId = toolUseId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Map<String, Object> getToolInput() {
        return toolInput;
    }

    public void setToolInput(Map<String, Object> toolInput) {
        this.toolInput = toolInput;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isTextDelta() {
        return type == StreamEventType.CONTENT_BLOCK_DELTA && text != null && toolUseId == null;
    }

    public boolean isToolUseStart() {
        return type == StreamEventType.CONTENT_BLOCK_START && toolUseId != null;
    }

    public boolean isToolInputDelta() {
        return type == StreamEventType.CONTENT_BLOCK_DELTA && toolUseId != null;
    }

    public boolean isToolUseStop() {
        return type == StreamEventType.CONTENT_BLOCK_STOP && toolUseId != null;
    }

    @Override
    public String toString() {
        return "StreamChunk{type=" + type + ", text='" + text + "', toolUseId='" + toolUseId + "', toolName='" + toolName + "'}";
    }
}
