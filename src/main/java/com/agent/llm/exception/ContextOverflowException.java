package com.agent.llm.exception;

public class ContextOverflowException extends LlmException {

    private final int currentTokens;
    private final int maxTokens;

    public ContextOverflowException(String message, int currentTokens, int maxTokens) {
        super(message, 413, "claude", true);
        this.currentTokens = currentTokens;
        this.maxTokens = maxTokens;
    }

    public ContextOverflowException(String message, Throwable cause, int currentTokens, int maxTokens) {
        super(message, cause, 413, "claude", true);
        this.currentTokens = currentTokens;
        this.maxTokens = maxTokens;
    }

    public int getCurrentTokens() {
        return currentTokens;
    }

    public int getMaxTokens() {
        return maxTokens;
    }
}
