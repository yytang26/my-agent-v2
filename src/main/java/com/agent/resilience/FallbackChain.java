package com.agent.resilience;

import com.agent.llm.LlmClient;
import com.agent.llm.exception.LlmException;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FallbackChain {

    private static final Logger logger = LoggerFactory.getLogger(FallbackChain.class);

    private final List<LlmClient> providers;

    public FallbackChain(List<LlmClient> providers) {
        this.providers = providers.stream()
                .filter(p -> !(p instanceof ResilientLlmClient))
                .toList();
    }

    public ChatResponse execute(List<ChatMessage> messages, ModelConfig config) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException("没有可用的 LLM Provider");
        }

        LlmException lastException = null;
        for (int i = 0; i < providers.size(); i++) {
            LlmClient provider = providers.get(i);
            try {
                logger.info("尝试使用 Provider {}: {}", i + 1, provider.getClass().getSimpleName());
                return provider.chat(messages, config);
            } catch (LlmException e) {
                lastException = e;
                logger.warn("Provider {} 失败 (status={}, retryable={}): {}",
                        i + 1, e.getStatusCode(), e.isRetryable(), e.getMessage());
                if (!e.isRetryable() || i == providers.size() - 1) {
                    break;
                }
                logger.info("切换到下一个 Provider...");
            }
        }

        throw lastException;
    }
}
