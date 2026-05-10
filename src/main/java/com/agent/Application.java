package com.agent;

import com.agent.config.AgentConfig;
import com.agent.config.ConfigManager;
import com.agent.llm.LlmClient;
import com.agent.resilience.RateLimiter;
import com.agent.resilience.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner run(LlmClient llmClient, ConfigManager configManager, RateLimiter rateLimiter, Environment env) {
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

            String prompt = "你好，请用一句话介绍你自己";
            String response = llmClient.ask(prompt);
            System.out.println("=== LLM 回复 ===");
            System.out.println(response);
            System.out.println("================");
        };
    }
}
