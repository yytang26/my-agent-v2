package com.agent.persistence;

import com.agent.memory.ConversationMemory;
import com.agent.memory.Message;
import com.agent.memory.MessageRole;
import com.agent.session.Session;
import com.agent.session.SessionMetadata;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 JPA 的持久化会话管理器
 * 作为 SessionManager 的补充，将 Session 持久化到数据库
 */
@Component
public class PersistentSessionManager {

    private static final Logger log = LoggerFactory.getLogger(PersistentSessionManager.class);

    private final SessionRepository sessionRepository;
    private final ConversationMemory conversationMemory;
    private final ObjectMapper objectMapper;

    public PersistentSessionManager(SessionRepository sessionRepository,
                                    ConversationMemory conversationMemory) {
        this.sessionRepository = sessionRepository;
        this.conversationMemory = conversationMemory;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 保存会话到数据库
     */
    public void save(Session session) {
        if (session == null) {
            return;
        }

        SessionEntity entity = toEntity(session);
        sessionRepository.save(entity);
        log.info("[PersistentSessionManager] 会话已保存到数据库: {}", session.getId());
    }

    /**
     * 从数据库加载会话
     */
    public Session load(String sessionId) {
        return sessionRepository.findById(sessionId)
                .map(this::toSession)
                .orElse(null);
    }

    /**
     * 列出所有会话（按更新时间倒序）
     */
    public List<SessionMetadata> listSessions() {
        List<SessionEntity> entities = sessionRepository.findAllByOrderByUpdatedAtDesc();
        List<SessionMetadata> result = new ArrayList<>();
        for (SessionEntity entity : entities) {
            result.add(new SessionMetadata(
                    entity.getId(),
                    entity.getTitle(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt(),
                    entity.getMessageCount(),
                    entity.getModel()
            ));
        }
        return result;
    }

    /**
     * 删除会话
     */
    public void delete(String sessionId) {
        sessionRepository.deleteById(sessionId);
        log.info("[PersistentSessionManager] 会话已删除: {}", sessionId);
    }

    private SessionEntity toEntity(Session session) {
        SessionEntity entity = new SessionEntity();
        entity.setId(session.getId());

        if (session.getMetadata() != null) {
            entity.setTitle(session.getMetadata().title());
            entity.setCreatedAt(session.getMetadata().createdAt());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setMessageCount(session.getMessages() != null ? session.getMessages().size() : 0);
            entity.setModel(session.getMetadata().model());
        } else {
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setMessageCount(0);
        }

        // 序列化消息为 JSON
        try {
            entity.setMessagesJson(objectMapper.writeValueAsString(session.getMessages()));
        } catch (Exception e) {
            log.error("[PersistentSessionManager] 序列化消息失败", e);
            entity.setMessagesJson("[]");
        }

        return entity;
    }

    private Session toSession(SessionEntity entity) {
        List<Message> messages = new ArrayList<>();
        try {
            if (entity.getMessagesJson() != null) {
                messages = objectMapper.readValue(entity.getMessagesJson(), new TypeReference<List<Message>>() {});
            }
        } catch (Exception e) {
            log.error("[PersistentSessionManager] 反序列化消息失败", e);
        }

        SessionMetadata metadata = new SessionMetadata(
                entity.getId(),
                entity.getTitle(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getMessageCount(),
                entity.getModel()
        );

        return new Session(entity.getId(), metadata, messages);
    }
}

