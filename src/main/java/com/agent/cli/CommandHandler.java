package com.agent.cli;

import com.agent.memory.ConversationMemory;
import com.agent.permission.PermissionManager;
import com.agent.permission.PermissionPolicy;
import com.agent.tool.ToolDefinition;
import com.agent.tool.ToolRegistry;
import com.agent.tracking.TokenTracker;
import com.agent.tracking.UsageSummary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CommandHandler {

    public static final String EXIT_SIGNAL = "__EXIT__";

    private final TokenTracker tokenTracker;
    private final ConversationMemory conversationMemory;
    private final ToolRegistry toolRegistry;
    private final PermissionManager permissionManager;

    public CommandHandler(TokenTracker tokenTracker,
                          ConversationMemory conversationMemory,
                          ToolRegistry toolRegistry,
                          PermissionManager permissionManager) {
        this.tokenTracker = tokenTracker;
        this.conversationMemory = conversationMemory;
        this.toolRegistry = toolRegistry;
        this.permissionManager = permissionManager;
    }

    public boolean isCommand(String input) {
        return input != null && input.trim().startsWith("/");
    }

    public String handleCommand(String input) {
        String cmd = input.trim();
        String[] parts = cmd.split("\\s+", 2);
        String command = parts[0];

        return switch (command) {
            case "/help" -> getHelpText();
            case "/clear" -> handleClear();
            case "/exit", "/quit" -> EXIT_SIGNAL;
            case "/cost" -> handleCost();
            case "/tools" -> handleTools();
            case "/permissions" -> handlePermissions();
            default -> "未知命令: " + command + "\n输入 /help 查看可用命令。";
        };
    }

    private String getHelpText() {
        return """
                可用命令:
                  /help        - 显示此帮助信息
                  /clear       - 清空当前对话记忆
                  /cost        - 显示 token 使用和费用统计
                  /tools       - 列出所有可用工具
                  /permissions - 显示当前权限规则和 session allow list
                  /exit        - 退出程序
                """;
    }

    private String handleClear() {
        conversationMemory.clear();
        return "对话记忆已清空。";
    }

    private String handleCost() {
        UsageSummary summary = tokenTracker.getSummary();
        return String.format("""
                Token 使用统计:
                  总 Token 数: %d
                  输入 Token:  %d
                  输出 Token:  %d
                  请求次数:    %d
                  总费用:      $%.4f
                """,
                summary.getTotalTokens(),
                summary.getTotalInputTokens(),
                summary.getTotalOutputTokens(),
                summary.getRequestCount(),
                summary.getTotalCostUsd()
        );
    }

    private String handleTools() {
        List<ToolDefinition> definitions = toolRegistry.getAllToolDefinitions();
        if (definitions.isEmpty()) {
            return "当前没有可用工具。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("可用工具列表:\n");
        for (ToolDefinition def : definitions) {
            sb.append(String.format("  • %s - %s%n", def.getName(), def.getDescription()));
        }
        return sb.toString().trim();
    }

    private String handlePermissions() {
        StringBuilder sb = new StringBuilder();

        sb.append("当前权限规则:\n");
        Map<String, PermissionPolicy> rules = permissionManager.getAllRules();
        if (rules.isEmpty()) {
            sb.append("  (无规则)\n");
        } else {
            for (Map.Entry<String, PermissionPolicy> entry : rules.entrySet()) {
                String icon = switch (entry.getValue()) {
                    case ALLOW -> "✓";
                    case ASK -> "?";
                    case DENY -> "✗";
                };
                sb.append(String.format("  %s %-20s -> %s%n", icon, entry.getKey(), entry.getValue()));
            }
        }

        Set<String> allowed = permissionManager.getSessionAllowList().getAllAllowed();
        sb.append("\nSession Allow List:\n");
        if (allowed.isEmpty()) {
            sb.append("  (无)\n");
        } else {
            for (String toolName : allowed) {
                sb.append(String.format("  ✓ %s%n", toolName));
            }
        }

        return sb.toString().trim();
    }
}
