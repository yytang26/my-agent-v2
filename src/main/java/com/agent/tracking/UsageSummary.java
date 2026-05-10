package com.agent.tracking;

public class UsageSummary {

    private int totalInputTokens;
    private int totalOutputTokens;
    private int totalTokens;
    private double totalCostUsd;
    private int requestCount;

    public int getTotalInputTokens() {
        return totalInputTokens;
    }

    public void setTotalInputTokens(int totalInputTokens) {
        this.totalInputTokens = totalInputTokens;
    }

    public int getTotalOutputTokens() {
        return totalOutputTokens;
    }

    public void setTotalOutputTokens(int totalOutputTokens) {
        this.totalOutputTokens = totalOutputTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public double getTotalCostUsd() {
        return totalCostUsd;
    }

    public void setTotalCostUsd(double totalCostUsd) {
        this.totalCostUsd = totalCostUsd;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
    }

    @Override
    public String toString() {
        return String.format(
                "本次会话: %d tokens (输入: %d, 输出: %d), 费用: $%.4f, 请求次数: %d",
                totalTokens, totalInputTokens, totalOutputTokens, totalCostUsd, requestCount
        );
    }
}
