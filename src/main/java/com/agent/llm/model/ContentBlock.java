package com.agent.llm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentBlock {

    private String type;
    private String text;
    private String id;
    private String name;
    private Map<String, Object> input;

    @JsonProperty("tool_use_id")
    private String toolUseId;

    @JsonProperty("is_error")
    private Boolean isError;

    public ContentBlock() {
    }

    public static ContentBlock text(String text) {
        ContentBlock block = new ContentBlock();
        block.setType("text");
        block.setText(text);
        return block;
    }

    public static ContentBlock toolUse(String id, String name, Map<String, Object> input) {
        ContentBlock block = new ContentBlock();
        block.setType("tool_use");
        block.setId(id);
        block.setName(name);
        block.setInput(input);
        return block;
    }

    public static ContentBlock toolResult(String toolUseId, String content, boolean isError) {
        ContentBlock block = new ContentBlock();
        block.setType("tool_result");
        block.setToolUseId(toolUseId);
        block.setText(content);
        block.setIsError(isError);
        return block;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }

    public String getToolUseId() {
        return toolUseId;
    }

    public void setToolUseId(String toolUseId) {
        this.toolUseId = toolUseId;
    }

    public Boolean getIsError() {
        return isError;
    }

    public void setIsError(Boolean isError) {
        this.isError = isError;
    }
}
