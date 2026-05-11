package com.agent.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 跨会话长期记忆
 * 可以记住用户名、偏好等重要信息
 */
@Component
public class LongTermMemory {

    private static final Logger log = LoggerFactory.getLogger(LongTermMemory.class);

    private final MemoryEntryRepository memoryEntryRepository;

    public LongTermMemory(MemoryEntryRepository memoryEntryRepository) {
        this.memoryEntryRepository = memoryEntryRepository;
    }

    /**
     * 存储信息
     */
    public void remember(String key, String value) {
        MemoryEntry entry = memoryEntryRepository.findByKey(key).orElse(null);
        if (entry == null) {
            entry = new MemoryEntry();
            entry.setId(UUID.randomUUID().toString().substring(0, 8));
            entry.setKey(key);
            entry.setCreatedAt(LocalDateTime.now());
        }
        entry.setValue(value);
        entry.setLastAccessedAt(LocalDateTime.now());
        memoryEntryRepository.save(entry);
        log.info("[LongTermMemory] 记住: {} = {}", key, value);
    }

    /**
     * 检索信息
     */
    public String recall(String key) {
        return memoryEntryRepository.findByKey(key)
                .map(entry -> {
                    entry.setLastAccessedAt(LocalDateTime.now());
                    memoryEntryRepository.save(entry);
                    return entry.getValue();
                })
                .orElse(null);
    }

    /**
     * 模糊搜索相关信息
     */
    public List<MemoryEntry> searchRelated(String query) {
        List<MemoryEntry> results = memoryEntryRepository.searchByQuery(query);
        log.info("[LongTermMemory] 搜索 '{}' 找到 {} 条结果", query, results.size());
        return results;
    }

    /**
     * 列出所有记忆
     */
    public List<MemoryEntry> listAll() {
        return memoryEntryRepository.findAllByOrderByLastAccessedAtDesc();
    }

    /**
     * 删除记忆
     */
    public void forget(String key) {
        memoryEntryRepository.findByKey(key).ifPresent(entry -> {
            memoryEntryRepository.delete(entry);
            log.info("[LongTermMemory] 遗忘: {}", key);
        });
    }
}
