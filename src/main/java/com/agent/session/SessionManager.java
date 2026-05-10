package com.agent.session;

import com.agent.memory.ConversationMemory;
import com.agent.memory.Message;
import com.agent.memory.MessageRole;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private final SessionSerializer sessionSerializer;
    private final ConversationMemory conversationMemory;
    private final Path sessionsDir;

    private Session currentSession;

    public SessionManager(SessionSerializer sessionSerializer,
                          ConversationMemory conversationMemory,
                          @Value("${agent.sessions-dir:${user.home}/.my-agent/sessions}") String sessionsDirPath) {
        this.sessionSerializer = sessionSerializer;
        this.conversationMemory = conversationMemory;
        this.sessionsDir = Path.of(sessionsDirPath);
    }

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(sessionsDir)) {
                Files.createDirectories(sessionsDir);
                log.info("[SessionManager] 创建会话存储目录: {}", sessionsDir);
            }
        } catch (IOException e) {
            log.error("[SessionManager] 无法创建会话存储目录: {}", sessionsDir, e);
        }
        createNew();
    }

    public Session createNew() {
        currentSession = new Session();
        currentSession.setMetadata(new SessionMetadata(
                currentSession.getId(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                0,
                null
        ));
        log.info("[SessionManager] 创建新会话: {}", currentSession.getId());
        return currentSession;
    }

    public void save(Session session) {
        if (session == null) {
            return;
        }
        Path filePath = sessionsDir.resolve(session.getId() + ".json");
        try {
            sessionSerializer.save(session, filePath);
            log.info("[SessionManager] 会话已保存: {}", filePath);
        } catch (IOException e) {
            log.error("[SessionManager] 保存会话失败: {}", session.getId(), e);
        }
    }

    public void autoSave() {
        if (currentSession == null) {
            return;
        }
        List<Message> messages = conversationMemory.getMessages();
        currentSession.setMessages(messages);
        currentSession.updateTitleFromFirstUserMessage();

        String model = currentSession.getMetadata() != null ? currentSession.getMetadata().model() : null;
        currentSession.setMetadata(new SessionMetadata(
                currentSession.getId(),
                currentSession.getMetadata() != null ? currentSession.getMetadata().title() : null,
                currentSession.getMetadata() != null ? currentSession.getMetadata().createdAt() : LocalDateTime.now(),
                LocalDateTime.now(),
                messages.size(),
                model
        ));
        save(currentSession);
    }

    public Session load(String sessionId) {
        Path filePath = sessionsDir.resolve(sessionId + ".json");
        if (!Files.exists(filePath)) {
            log.warn("[SessionManager] 会话文件不存在: {}", filePath);
            return null;
        }
        try {
            return sessionSerializer.load(filePath);
        } catch (IOException e) {
            log.error("[SessionManager] 加载会话失败: {}", sessionId, e);
            return null;
        }
    }

    public List<SessionMetadata> listSessions() {
        List<SessionMetadata> result = new ArrayList<>();
        if (!Files.exists(sessionsDir)) {
            return result;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sessionsDir, "*.json")) {
            for (Path path : stream) {
                try {
                    Session session = sessionSerializer.load(path);
                    if (session.getMetadata() != null) {
                        result.add(session.getMetadata());
                    }
                } catch (IOException e) {
                    log.warn("[SessionManager] 读取会话文件失败: {}", path, e);
                }
            }
        } catch (IOException e) {
            log.error("[SessionManager] 列出会话失败", e);
        }
        result.sort(Comparator.comparing(SessionMetadata::lastUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    public void resume(String sessionId) {
        Session session = load(sessionId);
        if (session == null) {
            log.warn("[SessionManager] 无法恢复会话: {}", sessionId);
            return;
        }
        conversationMemory.clear();
        for (Message msg : session.getMessages()) {
            if (msg.role() == MessageRole.SYSTEM) {
                conversationMemory.setSystemPrompt(msg.content());
            } else {
                conversationMemory.addMessage(msg);
            }
        }
        currentSession = session;
        currentSession.setMetadata(new SessionMetadata(
                currentSession.getId(),
                currentSession.getMetadata() != null ? currentSession.getMetadata().title() : null,
                currentSession.getMetadata() != null ? currentSession.getMetadata().createdAt() : LocalDateTime.now(),
                LocalDateTime.now(),
                session.getMessages().size(),
                currentSession.getMetadata() != null ? currentSession.getMetadata().model() : null
        ));
        log.info("[SessionManager] 恢复会话: {} ({} 条消息)", sessionId, session.getMessages().size());
    }

    public Session getCurrentSession() {
        return currentSession;
    }
}
