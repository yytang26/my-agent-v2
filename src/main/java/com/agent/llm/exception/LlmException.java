package com.agent.llm.exception;

public class LlmException extends RuntimeException {

    private final int statusCode;
    private final String provider;
    private final boolean retryable;

    public LlmException(String message, int statusCode, String provider, boolean retryable) {
        super(message);
        this.statusCode = statusCode;
        this.provider = provider;
        this.retryable = retryable;
    }

    public LlmException(String message, Throwable cause, int statusCode, String provider, boolean retryable) {
        super(message, cause);
        this.statusCode = statusCode;
        this.provider = provider;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getProvider() {
        return provider;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
