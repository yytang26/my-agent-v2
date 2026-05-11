package com.agent.security;

public class EscalationDecision {

    private final boolean requiresHuman;
    private final String message;
    private final EscalationLevel level;

    public EscalationDecision(boolean requiresHuman, String message, EscalationLevel level) {
        this.requiresHuman = requiresHuman;
        this.message = message;
        this.level = level;
    }

    public static EscalationDecision autoPass() {
        return new EscalationDecision(false, "Low risk operation, auto-approved", EscalationLevel.INFO);
    }

    public static EscalationDecision warn(String message) {
        return new EscalationDecision(false, message, EscalationLevel.WARN);
    }

    public static EscalationDecision block(String message) {
        return new EscalationDecision(true, message, EscalationLevel.BLOCK);
    }

    public boolean isRequiresHuman() {
        return requiresHuman;
    }

    public String getMessage() {
        return message;
    }

    public EscalationLevel getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return "EscalationDecision{requiresHuman=" + requiresHuman +
                ", level=" + level +
                ", message='" + message + '\'' + '}';
    }
}
