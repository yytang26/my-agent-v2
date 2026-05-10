package com.agent.tracking;

import org.springframework.stereotype.Component;

@Component
public class TokenTracker {

    private final CostCalculator costCalculator;

    private int totalInputTokens;
    private int totalOutputTokens;
    private int totalTokens;
    private double totalCostUsd;
    private int requestCount;

    public TokenTracker(CostCalculator costCalculator) {
        this.costCalculator = costCalculator;
    }

    public synchronized void recordUsage(String model, int inputTokens, int outputTokens) {
        double cost = costCalculator.calculateCost(model, inputTokens, outputTokens);
        this.totalInputTokens += inputTokens;
        this.totalOutputTokens += outputTokens;
        this.totalTokens += inputTokens + outputTokens;
        this.totalCostUsd += cost;
        this.requestCount++;
    }

    public synchronized UsageSummary getSummary() {
        UsageSummary summary = new UsageSummary();
        summary.setTotalInputTokens(totalInputTokens);
        summary.setTotalOutputTokens(totalOutputTokens);
        summary.setTotalTokens(totalTokens);
        summary.setTotalCostUsd(totalCostUsd);
        summary.setRequestCount(requestCount);
        return summary;
    }

    public synchronized void reset() {
        this.totalInputTokens = 0;
        this.totalOutputTokens = 0;
        this.totalTokens = 0;
        this.totalCostUsd = 0.0;
        this.requestCount = 0;
    }
}
