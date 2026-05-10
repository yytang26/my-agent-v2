package com.agent;

import com.agent.config.AgentConfig;
import com.agent.config.ConfigManager;
import com.agent.llm.LlmClient;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ModelConfig;
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
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner run(LlmClient llmClient, ConfigManager configManager, RateLimiter rateLimiter,
                                 TokenTracker tokenTracker, ConversationMemory memory, Environment env,
                                 ToolRegistry toolRegistry, ToolExecutor toolExecutor) {
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

            System.out.println("\n=== Tool 手动调用演示 ===");
            ToolResult result = toolExecutor.execute("get_current_time", "test-id-001",
                    Map.of("timezone", "Asia/Shanghai"));
            System.out.println("工具执行结果: " + result);
            System.out.println("====================");

            memory.setSystemPrompt("你是一个有帮助的 AI 助手。");

            String[] userPrompts = {
                    "我叫小明",
                    "我喜欢Java",
                    "我叫什么名字？",
                    "我喜欢什么编程语言？",
                    "目前对话用了多少token？",
                    "现在几点了？"
            };

            ModelConfig modelConfig = ModelConfig.builder().maxTokens(1024).build();

            for (int i = 0; i < userPrompts.length; i++) {
                String prompt = userPrompts[i];
                System.out.println("\n=== 第 " + (i + 1) + " 轮对话 ===");
                System.out.println("用户: " + prompt);

                memory.addMessage(Message.user(prompt));

                List<ChatMessage> chatMessages = memory.toChatMessages();
                ChatResponse response = llmClient.chat(chatMessages, modelConfig);
                String reply = response != null ? response.getFirstTextContent() : "(无回复)";
                if (reply == null) {
                    reply = "(助手返回了非文本内容)";
                }

                System.out.println("助手: " + reply);
                memory.addMessage(Message.assistant(reply));
            }

            System.out.println("\n=== 对话记忆统计 ===");
            System.out.println("消息数量: " + memory.size());
            System.out.println("估算 Token 数: " + memory.estimateTokenCount());
            System.out.println("====================");

            System.out.println("\n=== Token 使用统计 ===");
            System.out.println(tokenTracker.getSummary());
            System.out.println("====================");
        };
    }
}
