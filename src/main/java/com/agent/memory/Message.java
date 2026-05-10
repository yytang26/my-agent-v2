package com.agent.memory;

public record Message(MessageRole role, String content, long timestamp) {

    public Message {
        if (role == null) {
            throw new IllegalArgumentException("role cannot be null");
        }
        if (content == null) {
            throw new IllegalArgumentException("content cannot be null");
        }
    }

    public static Message user(String content) {
        return new Message(MessageRole.USER, content, System.currentTimeMillis());
    }

    public static Message assistant(String content) {
        return new Message(MessageRole.ASSISTANT, content, System.currentTimeMillis());
    }

    public static Message system(String content) {
        return new Message(MessageRole.SYSTEM, content, System.currentTimeMillis());
    }

    public static Message tool(String content) {
        return new Message(MessageRole.TOOL, content, System.currentTimeMillis());
    }
}
