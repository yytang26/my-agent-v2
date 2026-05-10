package com.agent;

import com.agent.config.AgentConfig;
import com.agent.config.ConfigManager;
import com.agent.llm.LlmClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner run(LlmClient llmClient, ConfigManager configManager) {
        return args -> {
            configManager.printConfigSources();
            AgentConfig config = configManager.getConfig();
            System.out.println("=== 当前生效配置 ===");
            System.out.println(config);
            System.out.println("====================");

            String prompt = "你好，请用一句话介绍你自己";
            String response = llmClient.ask(prompt);
            System.out.println("=== LLM 回复 ===");
            System.out.println(response);
            System.out.println("================");
        };
    }
}
