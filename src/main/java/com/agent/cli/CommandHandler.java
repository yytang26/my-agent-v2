package com.agent.cli;

import com.agent.memory.ConversationMemory;
import com.agent.permission.PermissionManager;
import com.agent.permission.PermissionPolicy;
import com.agent.session.SessionManager;
import com.agent.session.SessionMetadata;
import com.agent.tool.ToolDefinition;
import com.agent.tool.ToolRegistry;
import com.agent.tracking.TokenTracker;
import com.agent.tracking.UsageSummary;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CommandHandler {

    public static final String EXIT_SIGNAL = "__EXIT__";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TokenTracker tokenTracker;
    private final ConversationMemory conversationMemory;
    private final ToolRegistry toolRegistry;
    private final PermissionManager permissionManager;
    private final SessionManager sessionManager;

    public CommandHandler(TokenTracker tokenTracker,
                          ConversationMemory conversationMemory,
                          ToolRegistry toolRegistry,
                          PermissionManager permissionManager,
                          SessionManager sessionManager) {
        this.tokenTracker = tokenTracker;
        this.conversationMemory = conversationMemory;
        this.toolRegistry = toolRegistry;
        this.permissionManager = permissionManager;
        this.sessionManager = sessionManager;
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
            case "/save" -> handleSave();
            case "/sessions" -> handleSessions();
            case "/resume" -> handleResume(parts.length > 1 ? parts[1].trim() : null);
            case "/new" -> handleNew();
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
                  /save        - 手动保存当前会话
                  /sessions    - 列出所有历史会话
                  /resume <id> - 恢复指定会话
                  /new         - 开始新会话
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

    private String handleSave() {
        sessionManager.autoSave();
        var session = sessionManager.getCurrentSession();
        return String.format("会话已保存: %s", session != null ? session.getId() : "unknown");
    }

    private String handleSessions() {
        List<SessionMetadata> sessions = sessionManager.listSessions();
        if (sessions.isEmpty()) {
            return "没有历史会话。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("历史会话列表:\n");
        for (SessionMetadata meta : sessions) {
            String title = meta.title() != null ? meta.title() : "(无标题)";
            String updated = meta.lastUpdatedAt() != null ? meta.lastUpdatedAt().format(DATE_FMT) : "N/A";
            sb.append(String.format("  %s | %-20s | %s | %d 条消息%n",
                    meta.id(), title, updated, meta.messageCount()));
        }
        return sb.toString().trim();
    }

    private String handleResume(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "用法: /resume <session-id>";
        }
        sessionManager.resume(sessionId);
        var session = sessionManager.getCurrentSession();
        if (session != null && session.getId().equals(sessionId)) {
            return String.format("已恢复会话: %s (%d 条消息)",
                    sessionId,
                    session.getMessages() != null ? session.getMessages().size() : 0);
        }
        return "恢复会话失败: " + sessionId;
    }

    private String handleNew() {
        conversationMemory.clear();
        var session = sessionManager.createNew();
        return "已创建新会话: " + session.getId();
    }
}
