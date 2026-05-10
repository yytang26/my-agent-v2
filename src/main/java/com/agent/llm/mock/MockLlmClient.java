package com.agent.llm.mock;

import com.agent.llm.LlmClient;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ContentBlock;
import com.agent.llm.model.ModelConfig;
import com.agent.tool.ToolDefinition;
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
        return chat(messages, config, List.of());
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages, ModelConfig config, List<ToolDefinition> tools) {
        log.info("[MockLlmClient] 当前处于 Mock 模式，返回模拟回复 (tools: {})", tools != null ? tools.size() : 0);

        int messageCount = messages != null ? messages.size() : 0;
        String lastMessage = "";
        boolean hasToolResult = false;
        if (messages != null && !messages.isEmpty()) {
            ChatMessage last = messages.get(messages.size() - 1);
            lastMessage = last.getFirstTextContent() != null ? last.getFirstTextContent() : "";
            // 检查是否有 tool_result 消息
            for (ChatMessage msg : messages) {
                if (msg.getContentBlocks() != null) {
                    for (ContentBlock block : msg.getContentBlocks()) {
                        if ("tool_result".equals(block.getType())) {
                            hasToolResult = true;
                            break;
                        }
                    }
                }
            }
        }

        ChatResponse response = new ChatResponse();
        response.setId("mock-response-id");
        response.setModel("mock-model");

        boolean hasTimeTool = tools != null && tools.stream().anyMatch(t -> "get_current_time".equals(t.getName()));
        boolean isTimeQuery = lastMessage.contains("时间") || lastMessage.contains("time") || lastMessage.contains("几点");

        if (isTimeQuery && hasTimeTool && !hasToolResult) {
            // 第一次时间查询且工具可用：返回 tool_use
            log.info("[MockLlmClient] 检测到时间相关查询，返回模拟 tool_use");
            ContentBlock toolUseBlock = ContentBlock.toolUse(
                    "mock-tool-use-001",
                    "get_current_time",
                    Map.of("timezone", "Asia/Shanghai")
            );
            response.setContent(List.of(toolUseBlock));
        } else if (hasToolResult) {
            // 已收到 tool_result：返回总结文本
            log.info("[MockLlmClient] 检测到 tool_result，返回总结回复");
            String text = "[Mock] 根据工具返回的结果，当前时间是北京时间。我已经为您获取了最新时间信息。";
            response.setContent(List.of(ContentBlock.text(text)));
        } else {
            // 普通文本回复
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
