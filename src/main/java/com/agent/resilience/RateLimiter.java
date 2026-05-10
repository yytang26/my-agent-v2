package com.agent.resilience;

import com.agent.llm.exception.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class RateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);

    @Value("${resilience.rate-limiter.max-requests:50}")
    private int maxRequests;

    @Value("${resilience.rate-limiter.window-ms:60000}")
    private long windowMs;

    private final AtomicInteger currentRequests = new AtomicInteger(0);
    private volatile long windowStart = System.currentTimeMillis();
    private final Lock lock = new ReentrantLock();
    private final Condition available = lock.newCondition();

    public boolean tryAcquire() {
        lock.lock();
        try {
            refreshWindowIfNeeded();
            if (currentRequests.get() < maxRequests) {
                currentRequests.incrementAndGet();
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void acquire() {
        lock.lock();
        try {
            while (true) {
                refreshWindowIfNeeded();
                if (currentRequests.get() < maxRequests) {
                    currentRequests.incrementAndGet();
                    return;
                }
                long waitTime = windowMs - (System.currentTimeMillis() - windowStart);
                if (waitTime <= 0) {
                    continue;
                }
                logger.warn("RateLimiter 已超限 ({} / {}), 等待 {}ms 后重试", currentRequests.get(), maxRequests, waitTime);
                available.awaitNanos(waitTime * 1_000_000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RateLimitException("限流获取被中断", 429, "rate-limiter", 0);
        } finally {
            lock.unlock();
        }
    }

    private void refreshWindowIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - windowStart >= windowMs) {
            currentRequests.set(0);
            windowStart = now;
            available.signalAll();
        }
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public long getWindowMs() {
        return windowMs;
    }
}
