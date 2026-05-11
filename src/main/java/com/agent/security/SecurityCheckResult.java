package com.agent.security;

public class SecurityCheckResult {

    private final boolean allowed;
    private final boolean needsHumanApproval;
    private final boolean useSandbox;
    private final String message;
    private final RiskAssessment riskAssessment;
    private final InjectionResult injectionResult;
    private final EscalationDecision escalationDecision;

    public SecurityCheckResult(boolean allowed, boolean needsHumanApproval, boolean useSandbox,
                               String message, RiskAssessment riskAssessment,
                               InjectionResult injectionResult, EscalationDecision escalationDecision) {
        this.allowed = allowed;
        this.needsHumanApproval = needsHumanApproval;
        this.useSandbox = useSandbox;
        this.message = message;
        this.riskAssessment = riskAssessment;
        this.injectionResult = injectionResult;
        this.escalationDecision = escalationDecision;
    }

    public static SecurityCheckResult blocked(String message, RiskAssessment risk, InjectionResult injection) {
        return new SecurityCheckResult(false, false, false, message, risk, injection, null);
    }

    public static SecurityCheckResult approved(String message, RiskAssessment risk,
                                               InjectionResult injection, EscalationDecision escalation) {
        return new SecurityCheckResult(true, false, false, message, risk, injection, escalation);
    }

    public static SecurityCheckResult needsApproval(String message, RiskAssessment risk,
                                                    InjectionResult injection, EscalationDecision escalation) {
        return new SecurityCheckResult(true, true, false, message, risk, injection, escalation);
    }

    public static SecurityCheckResult useSandbox(String message, RiskAssessment risk,
                                                 InjectionResult injection, EscalationDecision escalation) {
        return new SecurityCheckResult(true, false, true, message, risk, injection, escalation);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean isNeedsHumanApproval() {
        return needsHumanApproval;
    }

    public boolean isUseSandbox() {
        return useSandbox;
    }

    public String getMessage() {
        return message;
    }

    public RiskAssessment getRiskAssessment() {
        return riskAssessment;
    }

    public InjectionResult getInjectionResult() {
        return injectionResult;
    }

    public EscalationDecision getEscalationDecision() {
        return escalationDecision;
    }

    @Override
    public String toString() {
        return "SecurityCheckResult{allowed=" + allowed +
                ", needsHumanApproval=" + needsHumanApproval +
                ", useSandbox=" + useSandbox +
                ", message='" + message + '\'' + '}';
    }
}
