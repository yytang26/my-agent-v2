package com.agent;

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
    public CommandLineRunner run(LlmClient llmClient) {
        return args -> {
            String prompt = "你好，请用一句话介绍你自己";
            String response = llmClient.ask(prompt);
            System.out.println("=== LLM 回复 ===");
            System.out.println(response);
            System.out.println("================");
        };
    }
}
