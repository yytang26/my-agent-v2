package com.agent.llm;

import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmClientConfiguration {
    // 集中管理 LLM Client 配置
    // 具体 Provider 的 Bean 由各自类上的 @ConditionalOnProperty 控制
}
