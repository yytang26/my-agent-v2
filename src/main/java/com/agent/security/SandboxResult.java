package com.agent.security;

public class SandboxResult {

    private final boolean allowed;
    private final String output;
    private final String blockedReason;

    public SandboxResult(boolean allowed, String output, String blockedReason) {
        this.allowed = allowed;
        this.output = output;
        this.blockedReason = blockedReason;
    }

    public static SandboxResult blocked(String reason) {
        return new SandboxResult(false, null, reason);
    }

    public static SandboxResult success(String output) {
        return new SandboxResult(true, output, null);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getOutput() {
        return output;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    @Override
    public String toString() {
        return "SandboxResult{allowed=" + allowed +
                ", output='" + (output != null ? output.substring(0, Math.min(100, output.length())) + "..." : null) + '\'' +
                ", blockedReason='" + blockedReason + '\'' + '}';
    }
}
