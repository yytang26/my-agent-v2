package com.agent;

import com.agent.config.AgentConfig;
import com.agent.config.ConfigManager;
import com.agent.core.AgentLoop;
import com.agent.core.AgentResponse;
import com.agent.core.AgentState;
import com.agent.core.TurnResult;
import com.agent.llm.LlmClient;
import com.agent.memory.ConversationMemory;
import com.agent.memory.Message;
import com.agent.resilience.RateLimiter;
import com.agent.resilience.RetryPolicy;
import com.agent.tool.ToolDefinition;
import com.agent.tool.ToolExecutor;
import com.agent.tool.ToolRegistry;
import com.agent.tool.ToolResult;
import com.agent.tracking.TokenTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner run(LlmClient llmClient, ConfigManager configManager, RateLimiter rateLimiter,
                                 TokenTracker tokenTracker, ConversationMemory memory, Environment env,
                                 ToolRegistry toolRegistry, ToolExecutor toolExecutor,
                                 AgentLoop agentLoop) {
        return args -> {
            configManager.printConfigSources();
            AgentConfig config = configManager.getConfig();
            System.out.println("=== 当前生效配置 ===");
            System.out.println(config);
            System.out.println("====================");

            RetryPolicy retryPolicy = RetryPolicy.builder().build();
            System.out.println("=== 重试策略配置 ===");
            System.out.println("maxRetries: " + retryPolicy.getMaxRetries());
            System.out.println("initialDelayMs: " + retryPolicy.getInitialDelayMs());
            System.out.println("backoffMultiplier: " + retryPolicy.getBackoffMultiplier());
            System.out.println("maxDelayMs: " + retryPolicy.getMaxDelayMs());
            System.out.println("jitterFactor: " + retryPolicy.getJitterFactor());
            System.out.println("====================");

            boolean isMock = Arrays.asList(env.getActiveProfiles()).contains("mock");
            if (isMock) {
                System.out.println("[Mock 模式] 跳过限流和降级逻辑演示");
            } else {
                System.out.println("=== 限流器配置 ===");
                System.out.println("maxRequests: " + rateLimiter.getMaxRequests());
                System.out.println("windowMs: " + rateLimiter.getWindowMs());
                System.out.println("====================");
            }

            // === Tool 框架演示 ===
            System.out.println("\n=== Tool 工具注册 ===");
            List<ToolDefinition> definitions = toolRegistry.getAllToolDefinitions();
            if (definitions.isEmpty()) {
                System.out.println("未注册任何工具");
            } else {
                System.out.println("已注册工具: " + definitions.stream().map(ToolDefinition::getName).toList());
                for (ToolDefinition def : definitions) {
                    System.out.println("  - " + def.getName() + ": " + def.getDescription());
                    System.out.println("    inputSchema: " + def.getInputSchema());
                }
            }
            System.out.println("====================");

            // === 文件系统工具演示 ===
            System.out.println("\n========== 文件系统工具演示 ==========");

            // 1. list_directory 列出项目根目录
            System.out.println("\n--- 1. list_directory (根目录) ---");
            ToolResult listResult = toolExecutor.execute("list_directory", "demo-1",
                    Map.of("path", "."));
            System.out.println(listResult.getContent());

            // 2. read_file 读取 pom.xml 前10行
            System.out.println("\n--- 2. read_file (pom.xml 前10行) ---");
            Map<String, Object> readArgs = new HashMap<>();
            readArgs.put("path", "pom.xml");
            readArgs.put("start_line", 1);
            readArgs.put("end_line", 10);
            ToolResult readResult = toolExecutor.execute("read_file", "demo-2", readArgs);
            System.out.println(readResult.getContent());

            // 3. write_file 创建 test-output.txt
            System.out.println("\n--- 3. write_file (test-output.txt) ---");
            ToolResult writeResult = toolExecutor.execute("write_file", "demo-3",
                    Map.of("path", "test-output.txt", "content", "Hello, Stage 09!\nThis is a test file.\n"));
            System.out.println(writeResult.getContent());

            // 4. edit_file 修改 test-output.txt
            System.out.println("\n--- 4. edit_file (修改 test-output.txt) ---");
            ToolResult editResult = toolExecutor.execute("edit_file", "demo-4",
                    Map.of("path", "test-output.txt", "old_text", "Stage 09",
                            "new_text", "File System Tools"));
            System.out.println(editResult.getContent());

            // 验证修改后的内容
            System.out.println("\n--- 验证修改后的内容 ---");
            ToolResult verifyResult = toolExecutor.execute("read_file", "demo-5",
                    Map.of("path", "test-output.txt"));
            System.out.println(verifyResult.getContent());

            // 5. 尝试读取 /etc/passwd -> 验证被 PathValidator 拦截
            System.out.println("\n--- 5. 路径安全验证 (/etc/passwd 应被拦截) ---");
            ToolResult securityResult = toolExecutor.execute("read_file", "demo-6",
                    Map.of("path", "/etc/passwd"));
            System.out.println(securityResult.getContent());

            // 清理演示生成的文件
            System.out.println("\n--- 清理演示文件 ---");
            Path testOutputPath = Path.of(System.getProperty("user.dir"), "test-output.txt");
            try {
                Files.deleteIfExists(testOutputPath);
                System.out.println("Deleted: test-output.txt");
            } catch (Exception e) {
                System.out.println("Failed to delete test-output.txt: " + e.getMessage());
            }
            System.out.println("=====================================");

            // === 代码工具演示 ===
            System.out.println("\n========== 代码工具演示 ==========");

            // 1. glob 列出所有 Java 文件
            System.out.println("\n--- 1. glob (**/*.java) ---");
            ToolResult globResult = toolExecutor.execute("glob", "demo-code-1",
                    Map.of("pattern", "**/*.java"));
            System.out.println(globResult.getContent());

            // 2. grep 搜索 TODO
            System.out.println("\n--- 2. grep (TODO) ---");
            ToolResult grepTodoResult = toolExecutor.execute("grep", "demo-code-2",
                    Map.of("pattern", "TODO"));
            System.out.println(grepTodoResult.getContent());

            // 3. grep 搜索 @Component
            System.out.println("\n--- 3. grep (@Component) ---");
            ToolResult grepComponentResult = toolExecutor.execute("grep", "demo-code-3",
                    Map.of("pattern", "@Component"));
            System.out.println(grepComponentResult.getContent());

            // 4. bash 执行 mvn --version
            System.out.println("\n--- 4. bash (mvn --version) ---");
            ToolResult bashResult = toolExecutor.execute("bash", "demo-code-4",
                    Map.of("command", "mvn --version"));
            System.out.println(bashResult.getContent());

            System.out.println("=====================================");

            // === Agent Loop 演示 1: 触发工具调用 ===
            memory.clear();
            memory.setSystemPrompt("你是一个有帮助的 AI 助手。");
            String prompt1 = "现在几点了？";
            System.out.println("\n========== Agent Loop 演示 1: 工具调用 ==========");
            System.out.println("用户: " + prompt1);
            System.out.println("------------------------------------------------");

            AgentResponse response1 = agentLoop.run(prompt1);
            printAgentResponse(response1);

            // === Agent Loop 演示 2: 普通问题（直接文本回复） ===
            memory.clear();
            memory.setSystemPrompt("你是一个有帮助的 AI 助手。");
            String prompt2 = "你好";
            System.out.println("\n========== Agent Loop 演示 2: 普通问题 ==========");
            System.out.println("用户: " + prompt2);
            System.out.println("------------------------------------------------");

            AgentResponse response2 = agentLoop.run(prompt2);
            printAgentResponse(response2);

            System.out.println("\n=== 对话记忆统计 ===");
            System.out.println("消息数量: " + memory.size());
            System.out.println("估算 Token 数: " + memory.estimateTokenCount());
            System.out.println("====================");

            System.out.println("\n=== Token 使用统计 ===");
            System.out.println(tokenTracker.getSummary());
            System.out.println("====================");
        };
    }

    private void printAgentResponse(AgentResponse response) {
        System.out.println("\n--- 每轮状态变化 ---");
        for (TurnResult turn : response.getTurns()) {
            System.out.println("  第 " + turn.getIteration() + " 轮: " + turn.getState());
            if (!turn.getToolCalls().isEmpty()) {
                System.out.println("    工具调用:");
                for (TurnResult.ToolCall tc : turn.getToolCalls()) {
                    System.out.println("      - " + tc.name() + " (id=" + tc.id() + ")");
                }
            }
            if (!turn.getToolResults().isEmpty()) {
                System.out.println("    工具结果:");
                for (com.agent.tool.ToolResult tr : turn.getToolResults()) {
                    System.out.println("      - " + tr.getToolUseId() + ": " + tr.getContent());
                }
            }
        }
        System.out.println("\n--- 最终结果 ---");
        System.out.println("助手: " + response.getFinalMessage());
        System.out.println("总迭代次数: " + response.getTotalIterations());
        System.out.println("是否达到最大迭代限制: " + response.isReachedMaxIterations());
        System.out.println("================================================");
    }
}
