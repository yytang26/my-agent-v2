package com.agent.llm.openai;

import com.agent.llm.LlmClient;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ModelConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "openai")
public class OpenAiClient implements LlmClient {

    @Override
    public ChatResponse chat(List<ChatMessage> messages, ModelConfig config) {
        throw new UnsupportedOperationException("OpenAI provider not yet implemented");
    }
}
