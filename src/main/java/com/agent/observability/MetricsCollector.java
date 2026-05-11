package com.agent.observability;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 系统指标收集器
 * 收集关键运行指标: 请求数、token、费用、响应时间、工具调用、错误率
 */
@Component
public class MetricsCollector {

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalTokens = new AtomicLong(0);
    private final AtomicLong totalInputTokens = new AtomicLong(0);
    private final AtomicLong totalOutputTokens = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalDurationMs = new AtomicLong(0);
    private final AtomicLong totalToolCalls = new AtomicLong(0);
    private final AtomicLong totalCostCents = new AtomicLong(0); // 美分，避免浮点误差

    private final Map<String, AtomicLong> toolCallCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();

    /**
     * 记录一次请求
     */
    public void recordRequest(long durationMs, int tokens) {
        totalRequests.incrementAndGet();
        totalDurationMs.addAndGet(durationMs);
        totalTokens.addAndGet(tokens);
    }

    /**
     * 记录 token 使用
     */
    public void recordTokenUsage(int inputTokens, int outputTokens) {
        totalInputTokens.addAndGet(inputTokens);
        totalOutputTokens.addAndGet(outputTokens);
    }

    /**
     * 记录工具调用
     */
    public void recordToolCall(String toolName) {
        totalToolCalls.incrementAndGet();
        toolCallCounts.computeIfAbsent(toolName, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 记录错误
     */
    public void recordError(String errorType) {
        totalErrors.incrementAndGet();
        errorCounts.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 记录费用（美元）
     */
    public void recordCost(double costUsd) {
        totalCostCents.addAndGet((long) (costUsd * 100));
    }

    /**
     * 获取指标摘要
     */
    public MetricsSummary getSummary() {
        long requests = totalRequests.get();
        double avgDurationMs = requests > 0 ? (double) totalDurationMs.get() / requests : 0;
        double errorRate = requests > 0 ? (double) totalErrors.get() / requests : 0;
        double totalCostUsd = totalCostCents.get() / 100.0;

        return new MetricsSummary(
                requests,
                totalTokens.get(),
                totalInputTokens.get(),
                totalOutputTokens.get(),
                totalCostUsd,
                avgDurationMs,
                totalToolCalls.get(),
                toolCallCounts,
                totalErrors.get(),
                errorRate,
                errorCounts
        );
    }

    /**
     * 重置所有指标
     */
    public void reset() {
        totalRequests.set(0);
        totalTokens.set(0);
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        totalErrors.set(0);
        totalDurationMs.set(0);
        totalToolCalls.set(0);
        totalCostCents.set(0);
        toolCallCounts.clear();
        errorCounts.clear();
    }

    /**
     * 指标摘要记录
     */
    public record MetricsSummary(
            long totalRequests,
            long totalTokens,
            long totalInputTokens,
            long totalOutputTokens,
            double totalCostUsd,
            double avgResponseTimeMs,
            long totalToolCalls,
            Map<String, AtomicLong> toolCallCounts,
            long totalErrors,
            double errorRate,
            Map<String, AtomicLong> errorCounts
    ) {}
}
