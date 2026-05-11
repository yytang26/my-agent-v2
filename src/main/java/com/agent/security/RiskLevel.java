package com.agent.security;

public enum RiskLevel {
    LOW(0.1),
    MEDIUM(0.5),
    HIGH(0.9),
    CRITICAL(1.0);

    private final double defaultScore;

    RiskLevel(double defaultScore) {
        this.defaultScore = defaultScore;
    }

    public double getDefaultScore() {
        return defaultScore;
    }
}
