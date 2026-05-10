package com.agent.session;

import com.agent.memory.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Session {

    private String id;
    private SessionMetadata metadata;
    private List<Message> messages = new ArrayList<>();

    public Session() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
    }

    public Session(String id, SessionMetadata metadata, List<Message> messages) {
        this.id = id;
        this.metadata = metadata;
        if (messages != null) {
            this.messages.addAll(messages);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SessionMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(SessionMetadata metadata) {
        this.metadata = metadata;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
    }

    public void updateTitleFromFirstUserMessage() {
        if (metadata != null && metadata.title() != null && !metadata.title().isBlank()) {
            return;
        }
        for (Message msg : messages) {
            if (msg.role().name().equalsIgnoreCase("USER")) {
                String content = msg.content();
                String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
                this.metadata = new SessionMetadata(
                        id,
                        title,
                        metadata != null ? metadata.createdAt() : null,
                        metadata != null ? metadata.lastUpdatedAt() : null,
                        messages.size(),
                        metadata != null ? metadata.model() : null
                );
                return;
            }
        }
    }
}
