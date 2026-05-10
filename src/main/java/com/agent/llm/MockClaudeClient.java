package com.agent.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mock")
public class MockClaudeClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(MockClaudeClient.class);

    @Override
    public String ask(String prompt) {
        log.info("[MockClaudeClient] 当前处于 Mock 模式，返回固定回复");
        return "[Mock] 我是一个 AI 助手 Mock 实现。你发送的消息是: " + prompt;
    }
}
