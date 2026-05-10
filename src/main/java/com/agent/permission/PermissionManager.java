package com.agent.permission;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PermissionManager {

    private final SessionAllowList sessionAllowList;
    private final InteractivePrompter interactivePrompter;
    private final Map<String, PermissionPolicy> defaultRules = new ConcurrentHashMap<>();

    public PermissionManager(SessionAllowList sessionAllowList,
                             InteractivePrompter interactivePrompter) {
        this.sessionAllowList = sessionAllowList;
        this.interactivePrompter = interactivePrompter;
    }

    @PostConstruct
    public void init() {
        // 读取类操作默认允许
        defaultRules.put("read_file", PermissionPolicy.ALLOW);
        defaultRules.put("list_directory", PermissionPolicy.ALLOW);
        defaultRules.put("get_current_time", PermissionPolicy.ALLOW);
        defaultRules.put("grep", PermissionPolicy.ALLOW);
        defaultRules.put("glob", PermissionPolicy.ALLOW);

        // 写入类操作需要询问
        defaultRules.put("write_file", PermissionPolicy.ASK);
        defaultRules.put("edit_file", PermissionPolicy.ASK);

        // 危险操作需要询问
        defaultRules.put("bash", PermissionPolicy.ASK);
    }

    public boolean checkPermission(String toolName, Map<String, Object> arguments) {
        // 1. Session allow list
        if (sessionAllowList.isAllowed(toolName)) {
            return true;
        }

        // 2. 获取该工具的权限策略
        PermissionPolicy policy = defaultRules.getOrDefault(toolName, PermissionPolicy.ASK);

        // 3. ALLOW -> 直接允许
        if (policy == PermissionPolicy.ALLOW) {
            return true;
        }

        // 4. DENY -> 直接拒绝
        if (policy == PermissionPolicy.DENY) {
            return false;
        }

        // 5. ASK -> 交互式询问
        PermissionResponse response = interactivePrompter.prompt(toolName, arguments);
        return switch (response) {
            case YES -> true;
            case NO -> false;
            case ALWAYS -> {
                sessionAllowList.add(toolName);
                yield true;
            }
            case DENY_FOREVER -> {
                defaultRules.put(toolName, PermissionPolicy.DENY);
                yield false;
            }
        };
    }

    public PermissionPolicy getPolicy(String toolName) {
        return defaultRules.getOrDefault(toolName, PermissionPolicy.ASK);
    }

    public void setPolicy(String toolName, PermissionPolicy policy) {
        defaultRules.put(toolName, policy);
    }

    public Map<String, PermissionPolicy> getAllRules() {
        return Map.copyOf(defaultRules);
    }

    public SessionAllowList getSessionAllowList() {
        return sessionAllowList;
    }
}
