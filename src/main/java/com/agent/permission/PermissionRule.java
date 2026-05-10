package com.agent.permission;

public class PermissionRule {

    private final String toolName;
    private final PermissionPolicy policy;
    private final String description;

    public PermissionRule(String toolName, PermissionPolicy policy, String description) {
        this.toolName = toolName;
        this.policy = policy;
        this.description = description;
    }

    public String getToolName() {
        return toolName;
    }

    public PermissionPolicy getPolicy() {
        return policy;
    }

    public String getDescription() {
        return description;
    }

    public static PermissionRule allow(String toolName) {
        return new PermissionRule(toolName, PermissionPolicy.ALLOW, null);
    }

    public static PermissionRule ask(String toolName) {
        return new PermissionRule(toolName, PermissionPolicy.ASK, null);
    }

    public static PermissionRule deny(String toolName) {
        return new PermissionRule(toolName, PermissionPolicy.DENY, null);
    }

    @Override
    public String toString() {
        return String.format("PermissionRule{toolName='%s', policy=%s}", toolName, policy);
    }
}
