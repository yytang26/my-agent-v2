package com.agent.permission;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionAllowList {

    private final Set<String> allowedTools = ConcurrentHashMap.newKeySet();

    public void add(String toolName) {
        allowedTools.add(toolName);
    }

    public boolean isAllowed(String toolName) {
        return allowedTools.contains(toolName);
    }

    public void clear() {
        allowedTools.clear();
    }

    public Set<String> getAllAllowed() {
        return Set.copyOf(allowedTools);
    }
}
