package com.agent.llm.mock;

import com.agent.llm.LlmClient;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ContentBlock;
import com.agent.llm.model.ModelConfig;
import com.agent.tracking.TokenTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Profile("mock")
public class MockLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(MockLlmClient.class);

    private final TokenTracker tokenTracker;

    public MockLlmClient(TokenTracker tokenTracker) {
        this.tokenTracker = tokenTracker;
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages, ModelConfig config) {
        log.info("[MockLlmClient] 当前处于 Mock 模式，返回模拟回复");

        int messageCount = messages != null ? messages.size() : 0;
        String lastMessage = "";
        if (messages != null && !messages.isEmpty()) {
            ChatMessage last = messages.get(messages.size() - 1);
            lastMessage = last.getFirstTextContent() != null ? last.getFirstTextContent() : "";
        }

        ChatResponse response = new ChatResponse();
        response.setId("mock-response-id");
        response.setModel("mock-model");

        boolean isTimeQuery = lastMessage.contains("时间") || lastMessage.contains("time") || lastMessage.contains("几点");

        if (isTimeQuery) {
            log.info("[MockLlmClient] 检测到时间相关查询，返回模拟 tool_use");
            ContentBlock toolUseBlock = ContentBlock.toolUse(
                    "mock-tool-use-001",
                    "get_current_time",
                    Map.of("timezone", "Asia/Shanghai")
            );
            response.setContent(List.of(toolUseBlock));
        } else {
            String text = "[Mock] 收到 " + messageCount + " 条历史消息。最新问题: " + lastMessage + "，模拟回答: 这是针对您问题的模拟回复，当前对话已累积 " + messageCount + " 条消息。";
            response.setContent(List.of(ContentBlock.text(text)));
        }

        int outputTokens = 0;
        for (ContentBlock block : response.getContent()) {
            if (block.getText() != null) {
                outputTokens += block.getText().length();
            } else {
                outputTokens += 50;
            }
        }

        ChatResponse.Usage usage = new ChatResponse.Usage();
        usage.setInputTokens(messageCount * 10);
        usage.setOutputTokens(outputTokens);
        response.setUsage(usage);

        if (tokenTracker != null && response.getUsage() != null) {
            tokenTracker.recordUsage(
                    response.getModel(),
                    response.getUsage().getInputTokens(),
                    response.getUsage().getOutputTokens()
            );
        }

        return response;
    }
}
