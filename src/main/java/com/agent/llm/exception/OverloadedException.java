package com.agent.llm.exception;

public class OverloadedException extends LlmException {

    public OverloadedException(String message, int statusCode, String provider) {
        super(message, statusCode, provider, true);
    }

    public OverloadedException(String message, Throwable cause, int statusCode, String provider) {
        super(message, cause, statusCode, provider, true);
    }
}
