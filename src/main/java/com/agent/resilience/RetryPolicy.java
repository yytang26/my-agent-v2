package com.agent.resilience;

public class RetryPolicy {

    private final int maxRetries;
    private final long initialDelayMs;
    private final double backoffMultiplier;
    private final long maxDelayMs;
    private final double jitterFactor;

    private RetryPolicy(Builder builder) {
        this.maxRetries = builder.maxRetries;
        this.initialDelayMs = builder.initialDelayMs;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.maxDelayMs = builder.maxDelayMs;
        this.jitterFactor = builder.jitterFactor;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public long getMaxDelayMs() {
        return maxDelayMs;
    }

    public double getJitterFactor() {
        return jitterFactor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int maxRetries = 3;
        private long initialDelayMs = 1000;
        private double backoffMultiplier = 2.0;
        private long maxDelayMs = 30000;
        private double jitterFactor = 0.1;

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder initialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
            return this;
        }

        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public Builder maxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }

        public Builder jitterFactor(double jitterFactor) {
            this.jitterFactor = jitterFactor;
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
}
