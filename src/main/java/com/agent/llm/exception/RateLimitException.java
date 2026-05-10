package com.agent.llm.exception;

public class RateLimitException extends LlmException {

    private final long retryAfterMs;

    public RateLimitException(String message, int statusCode, String provider, long retryAfterMs) {
        super(message, statusCode, provider, true);
        this.retryAfterMs = retryAfterMs;
    }

    public RateLimitException(String message, Throwable cause, int statusCode, String provider, long retryAfterMs) {
        super(message, cause, statusCode, provider, true);
        this.retryAfterMs = retryAfterMs;
    }

    public long getRetryAfterMs() {
        return retryAfterMs;
    }
}
