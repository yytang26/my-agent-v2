package com.agent.memory;

import com.agent.llm.model.ChatMessage;

import java.util.List;

public interface ConversationMemory {

    void addMessage(Message message);

    List<Message> getMessages();

    void setSystemPrompt(String prompt);

    int estimateTokenCount();

    void clear();

    int size();

    List<ChatMessage> toChatMessages();
}
