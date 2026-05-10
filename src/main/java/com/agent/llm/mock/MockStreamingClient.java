package com.agent.llm.mock;

import com.agent.llm.LlmClient;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ContentBlock;
import com.agent.llm.model.ModelConfig;
import com.agent.llm.model.StreamChunk;
import com.agent.tool.ToolDefinition;
import com.agent.tracking.TokenTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@Profile("mock")
@Primary
public class MockStreamingClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(MockStreamingClient.class);

    private final MockLlmClient mockLlmClient;
    private final TokenTracker tokenTracker;

    public MockStreamingClient(MockLlmClient mockLlmClient, TokenTracker tokenTracker) {
        this.mockLlmClient = mockLlmClient;
        this.tokenTracker = tokenTracker;
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages, ModelConfig config) {
        return mockLlmClient.chat(messages, config);
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages, ModelConfig config, List<ToolDefinition> tools) {
        return mockLlmClient.chat(messages, config, tools);
    }

    @Override
    public Flux<StreamChunk> chatStream(List<ChatMessage> messages, ModelConfig config, List<ToolDefinition> tools) {
        log.info("[MockStreamingClient] 当前处于 Mock 流式模式 (tools: {})", tools != null ? tools.size() : 0);

        int messageCount = messages != null ? messages.size() : 0;
        String lastMessage = "";
        boolean hasToolResult = false;
        if (messages != null && !messages.isEmpty()) {
            ChatMessage last = messages.get(messages.size() - 1);
            lastMessage = last.getFirstTextContent() != null ? last.getFirstTextContent() : "";
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

        boolean hasTimeTool = tools != null && tools.stream().anyMatch(t -> "get_current_time".equals(t.getName()));
        boolean isTimeQuery = lastMessage.contains("时间") || lastMessage.contains("time") || lastMessage.contains("几点");

        if (isTimeQuery && hasTimeTool && !hasToolResult) {
            // 模拟 tool_use 流
            log.info("[MockStreamingClient] 检测到时间相关查询，返回模拟 tool_use 流");
            return simulateToolUseStream();
        } else if (hasToolResult) {
            String text = "[Mock] 根据工具返回的结果，当前时间是北京时间。我已经为您获取了最新时间信息。";
            return simulateTextStream(text);
        } else {
            String text = "[Mock] 收到 " + messageCount + " 条历史消息。最新问题: " + lastMessage
                    + "，模拟回答: 这是针对您问题的模拟回复，当前对话已累积 " + messageCount + " 条消息。";
            return simulateTextStream(text);
        }
    }

    private Flux<StreamChunk> simulateTextStream(String text) {
        List<StreamChunk> chunks = new java.util.ArrayList<>();
        chunks.add(new StreamChunk(com.agent.llm.model.StreamEventType.MESSAGE_START));
        chunks.add(StreamChunk.textDelta(""));
        for (char c : text.toCharArray()) {
            chunks.add(StreamChunk.textDelta(String.valueOf(c)));
        }
        chunks.add(new StreamChunk(com.agent.llm.model.StreamEventType.CONTENT_BLOCK_STOP));
        chunks.add(new StreamChunk(com.agent.llm.model.StreamEventType.MESSAGE_STOP));

        return Flux.fromIterable(chunks)
                .delayElements(Duration.ofMillis(50));
    }

    private Flux<StreamChunk> simulateToolUseStream() {
        List<StreamChunk> chunks = new java.util.ArrayList<>();
        chunks.add(new StreamChunk(com.agent.llm.model.StreamEventType.MESSAGE_START));

        // 先输出一点文本
        String prefix = "我来帮您查询当前时间。";
        chunks.add(StreamChunk.textDelta(prefix));
        chunks.add(new StreamChunk(com.agent.llm.model.StreamEventType.CONTENT_BLOCK_STOP));

        // tool_use block
        StreamChunk toolStart = StreamChunk.toolUseStart("mock-tool-use-001", "get_current_time");
        chunks.add(toolStart);
        chunks.add(StreamChunk.inputJsonDelta("{\"timezone\": \"Asia/Shanghai\"}"));
        chunks.add(new StreamChunk(com.agent.llm.model.StreamEventType.CONTENT_BLOCK_STOP));

        chunks.add(new StreamChunk(com.agent.llm.model.StreamEventType.MESSAGE_STOP));

        return Flux.fromIterable(chunks)
                .delayElements(Duration.ofMillis(50));
    }
}
