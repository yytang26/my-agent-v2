package com.agent.security;

public class InjectionResult {

    private final boolean detected;
    private final String pattern;
    private final double confidence;
    private final String description;

    public InjectionResult(boolean detected, String pattern, double confidence, String description) {
        this.detected = detected;
        this.pattern = pattern;
        this.confidence = confidence;
        this.description = description;
    }

    public static InjectionResult clean() {
        return new InjectionResult(false, null, 0.0, "No injection detected");
    }

    public boolean isDetected() {
        return detected;
    }

    public String getPattern() {
        return pattern;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "InjectionResult{detected=" + detected +
                ", pattern='" + pattern + '\'' +
                ", confidence=" + confidence +
                ", description='" + description + '\'' + '}';
    }
}
