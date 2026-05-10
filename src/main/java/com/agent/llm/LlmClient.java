package com.agent.llm;

import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ModelConfig;
import com.agent.llm.model.StreamChunk;
import com.agent.tool.ToolDefinition;
import reactor.core.publisher.Flux;

import java.util.List;

public interface LlmClient {

    ChatResponse chat(List<ChatMessage> messages, ModelConfig config);

    default ChatResponse chat(List<ChatMessage> messages, ModelConfig config, List<ToolDefinition> tools) {
        return chat(messages, config);
    }

    default Flux<StreamChunk> chatStream(List<ChatMessage> messages, ModelConfig config, List<ToolDefinition> tools) {
        throw new UnsupportedOperationException("Streaming not supported");
    }

    default String ask(String prompt) {
        List<ChatMessage> messages = List.of(new ChatMessage("user", prompt));
        ModelConfig config = ModelConfig.builder()
                .maxTokens(1024)
                .build();
        ChatResponse response = chat(messages, config);
        if (response == null) {
            throw new RuntimeException("Empty response from LLM");
        }
        String text = response.getFirstTextContent();
        if (text == null) {
            throw new RuntimeException("No text content in LLM response");
        }
        return text;
    }
}
