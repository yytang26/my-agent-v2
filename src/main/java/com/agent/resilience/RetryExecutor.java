package com.agent.resilience;

import com.agent.llm.exception.LlmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.function.Supplier;

@Component
public class RetryExecutor {

    private static final Logger logger = LoggerFactory.getLogger(RetryExecutor.class);
    private final Random random = new Random();

    public <T> T executeWithRetry(Supplier<T> action, RetryPolicy policy) {
        int attempt = 0;
        LlmException lastException = null;

        while (attempt <= policy.getMaxRetries()) {
            try {
                return action.get();
            } catch (LlmException e) {
                lastException = e;
                if (!e.isRetryable() || attempt >= policy.getMaxRetries()) {
                    throw e;
                }

                long delay = computeDelay(attempt, policy);
                logger.warn("重试第 {} 次 (共 {} 次), 延迟 {}ms, 异常: {}",
                        attempt + 1, policy.getMaxRetries(), delay, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试被中断", ie);
                }
                attempt++;
            }
        }

        throw lastException;
    }

    private long computeDelay(int attempt, RetryPolicy policy) {
        long exponential = (long) (policy.getInitialDelayMs() * Math.pow(policy.getBackoffMultiplier(), attempt));
        long baseDelay = Math.min(exponential, policy.getMaxDelayMs());
        double jitter = baseDelay * policy.getJitterFactor() * (2 * random.nextDouble() - 1);
        return Math.max(0, baseDelay + (long) jitter);
    }
}
