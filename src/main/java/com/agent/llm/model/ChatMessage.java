package com.agent.llm.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    private String role;
    private String content;
    private List<ContentBlock> contentBlocks;

    public ChatMessage() {
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public ChatMessage(String role, List<ContentBlock> contentBlocks) {
        this.role = role;
        this.contentBlocks = contentBlocks;
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }

    public static ChatMessage assistant(List<ContentBlock> contentBlocks) {
        return new ChatMessage("assistant", contentBlocks);
    }

    public static ChatMessage toolResult(String toolUseId, String content, boolean isError) {
        return new ChatMessage("user", List.of(ContentBlock.toolResult(toolUseId, content, isError)));
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<ContentBlock> getContentBlocks() {
        return contentBlocks;
    }

    public void setContentBlocks(List<ContentBlock> contentBlocks) {
        this.contentBlocks = contentBlocks;
    }

    public String getFirstTextContent() {
        if (content != null && !content.isEmpty()) {
            return content;
        }
        if (contentBlocks != null && !contentBlocks.isEmpty()) {
            for (ContentBlock block : contentBlocks) {
                if ("text".equals(block.getType()) && block.getText() != null) {
                    return block.getText();
                }
            }
        }
        return null;
    }
}
