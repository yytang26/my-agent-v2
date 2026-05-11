package com.agent.cli;

import com.agent.core.AgentLoop;
import com.agent.core.AgentResponse;
import com.agent.eval.AgentEvaluator;
import com.agent.eval.EvalReport;
import com.agent.eval.EvalTestCase;
import com.agent.memory.ConversationMemory;
import com.agent.mcp.PluginLoader;
import com.agent.observability.MetricsCollector;
import com.agent.permission.PermissionManager;
import com.agent.permission.PermissionPolicy;
import com.agent.planner.TaskDAG;
import com.agent.planner.PlanTaskExecutor;
import com.agent.planner.TaskPlanner;
import com.agent.rag.Chunk;
import com.agent.rag.RagPipeline;
import com.agent.rag.VectorStore;
import com.agent.rag.advanced.AdvancedRagPipeline;
import com.agent.react.AgentMode;
import com.agent.routing.Intent;
import com.agent.routing.RoutingConfig;
import com.agent.session.SessionManager;
import com.agent.session.SessionMetadata;
import com.agent.template.CommandRegistry;
import com.agent.template.PromptTemplate;
import com.agent.template.SkillRegistry;
import com.agent.template.TemplateRenderer;
import com.agent.tool.ToolDefinition;
import com.agent.tool.ToolRegistry;
import com.agent.tracking.TokenTracker;
import com.agent.tracking.UsageSummary;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final AgentMode agentMode;
    private final RoutingConfig routingConfig;
    private final RagPipeline ragPipeline;
    private final AdvancedRagPipeline advancedRagPipeline;
    private final VectorStore vectorStore;
    private final com.agent.tool.builtin.RagTool ragTool;
    private final PluginLoader pluginLoader;
    private final AgentLoop agentLoop;
    private final SkillRegistry skillRegistry;
    private final CommandRegistry commandRegistry;
    private final TemplateRenderer templateRenderer;
    private final TaskPlanner taskPlanner;
    private final PlanTaskExecutor planTaskExecutor;
    private final AgentEvaluator agentEvaluator;
    private final MetricsCollector metricsCollector;

    private volatile TaskDAG currentPlanDag;

    public CommandHandler(TokenTracker tokenTracker,
                          ConversationMemory conversationMemory,
                          ToolRegistry toolRegistry,
                          PermissionManager permissionManager,
                          SessionManager sessionManager,
                          AgentMode agentMode,
                          RoutingConfig routingConfig,
                          RagPipeline ragPipeline,
                          AdvancedRagPipeline advancedRagPipeline,
                          VectorStore vectorStore,
                          com.agent.tool.builtin.RagTool ragTool,
                          PluginLoader pluginLoader,
                          AgentLoop agentLoop,
                          SkillRegistry skillRegistry,
                          CommandRegistry commandRegistry,
                          TemplateRenderer templateRenderer,
                          TaskPlanner taskPlanner,
                          PlanTaskExecutor planTaskExecutor,
                          AgentEvaluator agentEvaluator,
                          MetricsCollector metricsCollector) {
        this.tokenTracker = tokenTracker;
        this.conversationMemory = conversationMemory;
        this.toolRegistry = toolRegistry;
        this.permissionManager = permissionManager;
        this.sessionManager = sessionManager;
        this.agentMode = agentMode;
        this.routingConfig = routingConfig;
        this.ragPipeline = ragPipeline;
        this.advancedRagPipeline = advancedRagPipeline;
        this.vectorStore = vectorStore;
        this.ragTool = ragTool;
        this.pluginLoader = pluginLoader;
        this.agentLoop = agentLoop;
        this.skillRegistry = skillRegistry;
        this.commandRegistry = commandRegistry;
        this.templateRenderer = templateRenderer;
        this.taskPlanner = taskPlanner;
        this.planTaskExecutor = planTaskExecutor;
        this.agentEvaluator = agentEvaluator;
        this.metricsCollector = metricsCollector;
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
            case "/mode" -> handleMode(parts.length > 1 ? parts[1].trim() : null);
            case "/routing" -> handleRouting(parts.length > 1 ? parts[1].trim() : null);
            case "/rag" -> handleRag(parts.length > 1 ? parts[1].trim() : null);
            case "/plugins" -> handlePlugins(parts.length > 1 ? parts[1].trim() : null);
            case "/review" -> handleTemplateCommand("code-review", parts.length > 1 ? parts[1].trim() : null);
            case "/explain" -> handleTemplateCommand("explain-code", parts.length > 1 ? parts[1].trim() : null);
            case "/test" -> handleTemplateCommand("generate-test", parts.length > 1 ? parts[1].trim() : null);
            case "/skills" -> handleSkills();
            case "/commands" -> handleCommands();
            case "/plan" -> handlePlan(parts.length > 1 ? parts[1].trim() : null);
            case "/eval" -> handleEval(parts.length > 1 ? parts[1].trim() : null);
            case "/metrics" -> handleMetrics();
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
                  /mode        - 显示或切换运行模式
                  /routing     - 显示或开关意图路由
                  /rag         - RAG 知识库操作
                  /plugins     - 列出或重新加载 MCP 插件
                  /review <file>  - 代码审查
                  /explain <file> - 代码解释
                  /test <file>    - 生成测试
                  /skills         - 列出可用技能
                  /commands       - 列出可用命令
                  /plan <task>    - 将复杂任务分解为子任务并执行
                  /plan status    - 查看当前规划状态
                  /eval           - 运行评估套件
                  /eval report    - 显示最近一次评估报告
                  /metrics        - 显示系统指标
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

    private String handleMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "当前模式: " + agentMode.getCurrentMode() + "\n用法: /mode react | /mode native";
        }
        String lower = mode.toLowerCase();
        switch (lower) {
            case "react" -> {
                if (agentMode.isReact()) {
                    return "当前已经是 ReAct 模式。";
                }
                agentMode.setCurrentMode(AgentMode.REACT);
                conversationMemory.clear();
                return "已切换到 ReAct 模式。对话记忆已清空。";
            }
            case "native" -> {
                if (agentMode.isNative()) {
                    return "当前已经是原生 function calling 模式。";
                }
                agentMode.setCurrentMode(AgentMode.NATIVE);
                conversationMemory.clear();
                return "已切换到原生 function calling 模式。对话记忆已清空。";
            }
            default -> {
                return "未知模式: " + mode + "\n可用模式: react, native";
            }
        }
    }

    private String handleRouting(String arg) {
        if (arg == null || arg.isBlank()) {
            StringBuilder sb = new StringBuilder();
            sb.append("意图路由状态:\n");
            sb.append("  启用状态: ").append(routingConfig.isEnabled() ? "已启用" : "已禁用").append("\n");
            sb.append("  分类策略: ").append(routingConfig.isUseLlm() ? "LLM" : "规则匹配").append("\n");
            sb.append("\n各意图对应的工具集:\n");
            for (Intent intent : Intent.values()) {
                sb.append(String.format("  %-10s -> %s%n", intent, routingConfig.getToolsForIntent(intent)));
            }
            sb.append("\n用法: /routing on | /routing off");
            return sb.toString().trim();
        }
        String lower = arg.toLowerCase();
        switch (lower) {
            case "on", "enable", "true" -> {
                if (routingConfig.isEnabled()) {
                    return "意图路由已经是启用状态。";
                }
                routingConfig.setEnabled(true);
                return "意图路由已启用。后续请求将按意图激活对应工具集。";
            }
            case "off", "disable", "false" -> {
                if (!routingConfig.isEnabled()) {
                    return "意图路由已经是禁用状态。";
                }
                routingConfig.setEnabled(false);
                return "意图路由已禁用。所有工具将对每次请求可用。";
            }
            default -> {
                return "未知参数: " + arg + "\n用法: /routing on | /routing off";
            }
        }
    }

    private String handleRag(String args) {
        if (args == null || args.isBlank()) {
            return """
                    RAG 知识库操作:
                      /rag index <path> [pattern]  - 索引文件或目录
                      /rag search <query>          - 搜索知识库
                      /rag status                  - 显示索引状态
                      /rag mode <basic|advanced>   - 切换 RAG 模式
                    """;
        }

        String[] parts = args.split("\\s+", 2);
        String subCommand = parts[0];
        String rest = parts.length > 1 ? parts[1].trim() : null;

        return switch (subCommand) {
            case "index" -> handleRagIndex(rest);
            case "search" -> handleRagSearch(rest);
            case "status" -> handleRagStatus();
            case "mode" -> handleRagMode(rest);
            default -> "未知子命令: " + subCommand + "\n用法: /rag index <path> | /rag search <query> | /rag status | /rag mode <basic|advanced>";
        };
    }

    private String handleRagIndex(String rest) {
        if (rest == null || rest.isBlank()) {
            return "用法: /rag index <path> [pattern]";
        }

        String[] parts = rest.split("\\s+", 2);
        String path = parts[0];
        String pattern = parts.length > 1 ? parts[1].trim() : null;

        try {
            java.nio.file.Path filePath = java.nio.file.Path.of(path).toAbsolutePath().normalize();
            if (!java.nio.file.Files.exists(filePath)) {
                return "Error: 路径不存在: " + path;
            }

            if (java.nio.file.Files.isDirectory(filePath)) {
                ragPipeline.indexDirectory(path, pattern);
                return "已索引目录: " + path + (pattern != null ? " (模式: " + pattern + ")" : "");
            } else {
                ragPipeline.indexFile(path);
                return "已索引文件: " + path;
            }
        } catch (Exception e) {
            return "索引失败: " + e.getMessage();
        }
    }

    private String handleRagSearch(String query) {
        if (query == null || query.isBlank()) {
            return "用法: /rag search <query>";
        }

        List<Chunk> chunks = ragPipeline.retrieve(query, 5);
        if (chunks.isEmpty()) {
            return "未找到相关内容。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("搜索结果 (共 ").append(chunks.size()).append(" 条):\n\n");
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            sb.append(i + 1).append(". ");
            sb.append("[").append(chunk.getSourceFile());
            sb.append(" 行").append(chunk.getStartLine());
            sb.append("-").append(chunk.getEndLine()).append("]\n");
            sb.append(chunk.getContent()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String handleRagStatus() {
        int size = vectorStore.size();
        String mode = ragTool.getMode();
        return "知识库状态:\n  已索引 chunk 数量: " + size + "\n  当前 RAG 模式: " + mode;
    }

    private String handleRagMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "当前 RAG 模式: " + ragTool.getMode() + "\n用法: /rag mode basic | /rag mode advanced";
        }
        String lower = mode.toLowerCase();
        switch (lower) {
            case "basic" -> {
                if ("basic".equalsIgnoreCase(ragTool.getMode())) {
                    return "当前已经是 basic 模式。";
                }
                ragTool.setMode("basic");
                return "已切换到 basic RAG 模式。";
            }
            case "advanced" -> {
                if ("advanced".equalsIgnoreCase(ragTool.getMode())) {
                    return "当前已经是 advanced 模式。";
                }
                ragTool.setMode("advanced");
                return "已切换到 advanced RAG 模式（多路召回 + RRF 融合）。";
            }
            default -> {
                return "未知模式: " + mode + "\n可用模式: basic, advanced";
            }
        }
    }

    private String handlePlugins(String arg) {
        if (arg != null && arg.equalsIgnoreCase("reload")) {
            pluginLoader.reload();
            return "MCP 插件已重新加载。";
        }

        Map<String, List<String>> plugins = pluginLoader.getLoadedPlugins();
        if (plugins.isEmpty()) {
            return "当前没有加载的 MCP 插件。\n用法: /plugins reload - 重新加载插件";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("已加载的 MCP 插件:\n");
        for (Map.Entry<String, List<String>> entry : plugins.entrySet()) {
            sb.append(String.format("  [%s] (%d 个工具)%n", entry.getKey(), entry.getValue().size()));
            for (String toolName : entry.getValue()) {
                sb.append(String.format("    - %s%n", toolName));
            }
        }
        sb.append("\n用法: /plugins reload - 重新加载插件");
        return sb.toString().trim();
    }

    private String handleTemplateCommand(String skillName, String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "用法: /" + skillName.replace("-", " ") + " <file>";
        }

        Optional<PromptTemplate> skillOpt = skillRegistry.getSkill(skillName);
        if (skillOpt.isEmpty()) {
            return "未找到技能模板: " + skillName;
        }

        Path path = Path.of(filePath).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            return "Error: 文件不存在: " + filePath;
        }

        String content;
        try {
            content = Files.readString(path);
        } catch (Exception e) {
            return "Error: 读取文件失败: " + e.getMessage();
        }

        PromptTemplate template = skillOpt.get();
        Map<String, String> values = Map.of(
                "file_path", filePath,
                "code_content", content
        );
        String prompt = templateRenderer.render(template.getTemplate(), values, template.getVariables());

        AgentResponse response = agentLoop.run(prompt);
        return response.getFinalMessage();
    }

    private String handleSkills() {
        List<PromptTemplate> skills = skillRegistry.getSkills();
        if (skills.isEmpty()) {
            return "当前没有可用技能。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("可用技能列表:\n");
        for (PromptTemplate skill : skills) {
            sb.append(String.format("  • %s - %s%n", skill.getName(), skill.getDescription()));
        }
        return sb.toString().trim();
    }

    private String handleCommands() {
        List<String> commands = commandRegistry.listCommands();
        StringBuilder sb = new StringBuilder();
        sb.append("可用命令模板:\n");
        for (String cmd : commands) {
            sb.append(String.format("  %s%n", cmd));
        }
        return sb.toString().trim();
    }

    private String handlePlan(String arg) {
        if (arg == null || arg.isBlank()) {
            return """
                    用法:
                      /plan <task>    - 将复杂任务分解为子任务并执行
                      /plan status    - 查看当前规划状态
                    例如: /plan 分析这个项目的代码结构并生成文档
                    """;
        }

        if ("status".equalsIgnoreCase(arg)) {
            if (currentPlanDag == null) {
                return "当前没有正在执行的任务规划。\n用法: /plan <task> - 开始新的任务规划";
            }
            return currentPlanDag.getSummary();
        }

        // 开始新的任务规划
        try {
            var dag = taskPlanner.plan(arg);
            currentPlanDag = dag;
            StringBuilder sb = new StringBuilder();
            sb.append("=== 任务规划完成 ===\n\n");
            sb.append(dag.getSummary()).append("\n\n");
            sb.append("开始执行...\n");

            var resultDag = planTaskExecutor.execute(dag);
            currentPlanDag = resultDag;

            sb.append("\n=== 执行完成 ===\n\n");
            sb.append(resultDag.getSummary());

            if (resultDag.isComplete()) {
                sb.append("\n\n=== 最终结果汇总 ===\n");
                for (var task : resultDag.getTasks().values()) {
                    if (task.getResult() != null) {
                        sb.append(String.format("[%s] %s%n%n", task.getId(), task.getResult()));
                    }
                }
            }

            return sb.toString().trim();
        } catch (Exception e) {
            return "任务规划/执行失败: " + e.getMessage();
        }
    }

    private String handleEval(String arg) {
        if (arg != null && arg.equalsIgnoreCase("report")) {
            EvalReport report = agentEvaluator.getLastReport();
            if (report == null) {
                return "还没有运行过评估。使用 /eval 运行评估套件。";
            }
            return report.toString();
        }

        // 运行评估
        List<EvalTestCase> testCases = agentEvaluator.loadDefaultTestCases();
        if (testCases.isEmpty()) {
            return "未找到测试用例。";
        }

        EvalReport report = agentEvaluator.evaluate(testCases);
        return report.toString();
    }

    private String handleMetrics() {
        MetricsCollector.MetricsSummary summary = metricsCollector.getSummary();

        StringBuilder sb = new StringBuilder();
        sb.append("=== 系统指标 ===\n\n");
        sb.append(String.format("总请求数:     %d%n", summary.totalRequests()));
        sb.append(String.format("总 Token:     %d (输入: %d, 输出: %d)%n",
                summary.totalTokens(), summary.totalInputTokens(), summary.totalOutputTokens()));
        sb.append(String.format("总费用:       $%.4f%n", summary.totalCostUsd()));
        sb.append(String.format("平均响应时间: %.1f ms%n", summary.avgResponseTimeMs()));
        sb.append(String.format("工具调用次数: %d%n", summary.totalToolCalls()));
        sb.append(String.format("错误次数:     %d (错误率: %.1f%%)%n",
                summary.totalErrors(), summary.errorRate() * 100));

        if (!summary.toolCallCounts().isEmpty()) {
            sb.append("\n工具调用统计:\n");
            summary.toolCallCounts().entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                    .forEach(entry -> sb.append(String.format("  %-20s: %d%n", entry.getKey(), entry.getValue().get())));
        }

        if (!summary.errorCounts().isEmpty()) {
            sb.append("\n错误分类统计:\n");
            summary.errorCounts().entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                    .forEach(entry -> sb.append(String.format("  %-20s: %d%n", entry.getKey(), entry.getValue().get())));
        }

        return sb.toString().trim();
    }
}
