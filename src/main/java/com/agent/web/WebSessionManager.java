package com.agent.web;

import com.agent.core.AgentLoopConfig;
import com.agent.memory.ConversationMemory;
import com.agent.memory.InMemoryConversationMemory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSessionManager {

    private final Map<String, WebSession> sessions = new ConcurrentHashMap<>();
    private final AgentLoopConfig agentLoopConfig;

    public WebSessionManager(AgentLoopConfig agentLoopConfig) {
        this.agentLoopConfig = agentLoopConfig;
    }

    public WebSession createSession() {
        String sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        InMemoryConversationMemory memory = new InMemoryConversationMemory();
        WebSession session = new WebSession(sessionId, memory, agentLoopConfig);
        sessions.put(sessionId, session);
        return session;
    }

    public WebSession getSession(String id) {
        return sessions.get(id);
    }

    public List<WebSessionInfo> listSessions() {
        List<WebSessionInfo> result = new ArrayList<>();
        for (WebSession session : sessions.values()) {
            String title = null;
            var messages = session.memory().getMessages();
            for (var msg : messages) {
                if ("USER".equalsIgnoreCase(msg.role().name())) {
                    title = msg.content();
                    if (title.length() > 30) {
                        title = title.substring(0, 30) + "...";
                    }
                    break;
                }
            }
            result.add(new WebSessionInfo(
                    session.sessionId(),
                    session.memory().size(),
                    session.createdAt(),
                    title != null ? title : "新会话"
            ));
        }
        result.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return result;
    }

    public void deleteSession(String id) {
        sessions.remove(id);
    }

    public record WebSession(String sessionId, ConversationMemory memory, AgentLoopConfig config, long createdAt) {
        public WebSession(String sessionId, ConversationMemory memory, AgentLoopConfig config) {
            this(sessionId, memory, config, System.currentTimeMillis());
        }
    }
}
