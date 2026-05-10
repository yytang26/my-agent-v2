package com.agent.memory;

import com.agent.llm.model.ChatMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class InMemoryConversationMemory implements ConversationMemory {

    private final List<Message> messages = new ArrayList<>();
    private String systemPrompt = null;

    @Override
    public synchronized void addMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }
        messages.add(message);
    }

    @Override
    public synchronized List<Message> getMessages() {
        List<Message> all = new ArrayList<>();
        if (systemPrompt != null) {
            all.add(Message.system(systemPrompt));
        }
        all.addAll(messages);
        return Collections.unmodifiableList(all);
    }

    @Override
    public synchronized void setSystemPrompt(String prompt) {
        this.systemPrompt = prompt;
    }

    @Override
    public synchronized int estimateTokenCount() {
        int totalChars = 0;
        if (systemPrompt != null) {
            totalChars += systemPrompt.length();
        }
        for (Message msg : messages) {
            totalChars += msg.content().length();
        }
        return totalChars / 4;
    }

    @Override
    public synchronized void clear() {
        messages.clear();
        systemPrompt = null;
    }

    @Override
    public synchronized int size() {
        return messages.size();
    }

    @Override
    public synchronized List<ChatMessage> toChatMessages() {
        List<ChatMessage> result = new ArrayList<>();
        if (systemPrompt != null) {
            result.add(new ChatMessage("system", systemPrompt));
        }
        for (Message msg : messages) {
            String roleName = msg.role().name().toLowerCase();
            result.add(new ChatMessage(roleName, msg.content()));
        }
        return result;
    }
}
