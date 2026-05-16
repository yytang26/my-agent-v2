package com.agent.llm.mock;

import com.agent.llm.LlmClient;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("mock")
public class MockLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(MockLlmClient.class);

    @Override
    public ChatResponse chat(List<ChatMessage> messages, ModelConfig config) {
        log.info("[MockLlmClient] 当前处于 Mock 模式，返回模拟回复");

        int messageCount = messages != null ? messages.size() : 0;
        String lastMessage = "";
        if (messages != null && !messages.isEmpty()) {
            ChatMessage last = messages.get(messages.size() - 1);
            lastMessage = last.getContent();
        }

        String text = "[Mock] 收到 " + messageCount + " 条消息，最后一条: " + lastMessage;

        ChatResponse response = new ChatResponse();
        response.setId("mock-response-id");
        response.setModel("mock-model");

        ChatResponse.ContentBlock contentBlock = new ChatResponse.ContentBlock();
        contentBlock.setType("text");
        contentBlock.setText(text);
        response.setContent(List.of(contentBlock));

        ChatResponse.Usage usage = new ChatResponse.Usage();
        usage.setInputTokens(messageCount * 10);
        usage.setOutputTokens(text.length());
        response.setUsage(usage);

        return response;
    }
}
