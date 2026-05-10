package com.agent.resilience;

import com.agent.llm.LlmClient;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ModelConfig;
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

    @Value("${resilience.retry.enabled:true}")
    private boolean retryEnabled;

    @Value("${resilience.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    public ResilientLlmClient(RetryExecutor retryExecutor,
                              RateLimiter rateLimiter,
                              FallbackChain fallbackChain) {
        this.retryExecutor = retryExecutor;
        this.rateLimiter = rateLimiter;
        this.fallbackChain = fallbackChain;
        this.defaultRetryPolicy = RetryPolicy.builder().build();
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages, ModelConfig config) {
        if (rateLimitEnabled) {
            rateLimiter.acquire();
        }

        if (retryEnabled) {
            return retryExecutor.executeWithRetry(
                    () -> fallbackChain.execute(messages, config),
                    defaultRetryPolicy
            );
        } else {
            return fallbackChain.execute(messages, config);
        }
    }
}
