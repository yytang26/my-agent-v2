package com.agent.llm.exception;

public class AuthenticationException extends LlmException {

    public AuthenticationException(String message, int statusCode, String provider) {
        super(message, statusCode, provider, false);
    }

    public AuthenticationException(String message, Throwable cause, int statusCode, String provider) {
        super(message, cause, statusCode, provider, false);
    }
}
