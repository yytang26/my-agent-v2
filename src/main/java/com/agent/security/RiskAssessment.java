package com.agent.security;

public class RiskAssessment {

    private final RiskLevel level;
    private final double score;
    private final String reason;

    public RiskAssessment(RiskLevel level, double score, String reason) {
        this.level = level;
        this.score = score;
        this.reason = reason;
    }

    public static RiskAssessment low(String reason) {
        return new RiskAssessment(RiskLevel.LOW, RiskLevel.LOW.getDefaultScore(), reason);
    }

    public static RiskAssessment medium(String reason) {
        return new RiskAssessment(RiskLevel.MEDIUM, RiskLevel.MEDIUM.getDefaultScore(), reason);
    }

    public static RiskAssessment high(String reason) {
        return new RiskAssessment(RiskLevel.HIGH, RiskLevel.HIGH.getDefaultScore(), reason);
    }

    public static RiskAssessment critical(String reason) {
        return new RiskAssessment(RiskLevel.CRITICAL, RiskLevel.CRITICAL.getDefaultScore(), reason);
    }

    public RiskLevel getLevel() {
        return level;
    }

    public double getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "RiskAssessment{level=" + level +
                ", score=" + score +
                ", reason='" + reason + '\'' + '}';
    }
}
