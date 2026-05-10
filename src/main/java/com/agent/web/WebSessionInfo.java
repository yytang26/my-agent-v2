package com.agent.web;

public class WebSessionInfo {

    private String sessionId;
    private int messageCount;
    private long createdAt;
    private String title;

    public WebSessionInfo() {
    }

    public WebSessionInfo(String sessionId, int messageCount, long createdAt, String title) {
        this.sessionId = sessionId;
        this.messageCount = messageCount;
        this.createdAt = createdAt;
        this.title = title;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
