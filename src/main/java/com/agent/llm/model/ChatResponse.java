package com.agent.llm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ChatResponse {

    private String id;
    private List<ContentBlock> content;
    private String model;
    private Usage usage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<ContentBlock> getContent() {
        return content;
    }

    public void setContent(List<ContentBlock> content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public String getFirstTextContent() {
        if (content == null || content.isEmpty()) {
            return null;
        }
        for (ContentBlock block : content) {
            if ("text".equals(block.getType()) && block.getText() != null) {
                return block.getText();
            }
        }
        return null;
    }

    public static class Usage {

        @JsonProperty("input_tokens")
        private int inputTokens;

        @JsonProperty("output_tokens")
        private int outputTokens;

        public int getInputTokens() {
            return inputTokens;
        }

        public void setInputTokens(int inputTokens) {
            this.inputTokens = inputTokens;
        }

        public int getOutputTokens() {
            return outputTokens;
        }

        public void setOutputTokens(int outputTokens) {
            this.outputTokens = outputTokens;
        }
    }
}
