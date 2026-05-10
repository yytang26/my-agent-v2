package com.agent.resilience;

import com.agent.llm.LlmClient;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ModelConfig;
import com.agent.tracking.TokenTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
@Profile("!mock")
public class ResilientLlmClient implements LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(ResilientLlmClient.class);

    private final RetryExecutor retryExecutor;
    private final RateLimiter rateLimiter;
    private final FallbackChain fallbackChain;
    private final RetryPolicy defaultRetryPolicy;
    private final TokenTracker tokenTracker;

    @Value("${resilience.retry.enabled:true}")
    private boolean retryEnabled;

    @Value("${resilience.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    public ResilientLlmClient(RetryExecutor retryExecutor,
                              RateLimiter rateLimiter,
                              FallbackChain fallbackChain,
                              TokenTracker tokenTracker) {
        this.retryExecutor = retryExecutor;
        this.rateLimiter = rateLimiter;
        this.fallbackChain = fallbackChain;
        this.defaultRetryPolicy = RetryPolicy.builder().build();
        this.tokenTracker = tokenTracker;
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages, ModelConfig config) {
        if (rateLimitEnabled) {
            rateLimiter.acquire();
        }

        ChatResponse response;
        if (retryEnabled) {
            response = retryExecutor.executeWithRetry(
                    () -> fallbackChain.execute(messages, config),
                    defaultRetryPolicy
            );
        } else {
            response = fallbackChain.execute(messages, config);
        }

        if (tokenTracker != null && response != null && response.getUsage() != null) {
            String model = response.getModel() != null ? response.getModel() : config.getName();
            tokenTracker.recordUsage(
                    model != null ? model : "unknown",
                    response.getUsage().getInputTokens(),
                    response.getUsage().getOutputTokens()
            );
        }

        return response;
    }
}
