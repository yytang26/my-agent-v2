package com.agent.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SecurityGateway {

    private static final Logger log = LoggerFactory.getLogger(SecurityGateway.class);

    private final InjectionDetector injectionDetector;
    private final RiskScorer riskScorer;
    private final SandboxExecutor sandboxExecutor;
    private final HumanEscalation humanEscalation;
    private final OutputSanitizer outputSanitizer;

    public SecurityGateway(InjectionDetector injectionDetector,
                           RiskScorer riskScorer,
                           SandboxExecutor sandboxExecutor,
                           HumanEscalation humanEscalation,
                           OutputSanitizer outputSanitizer) {
        this.injectionDetector = injectionDetector;
        this.riskScorer = riskScorer;
        this.sandboxExecutor = sandboxExecutor;
        this.humanEscalation = humanEscalation;
        this.outputSanitizer = outputSanitizer;
    }

    public SecurityCheckResult check(String toolName, Map<String, Object> arguments, String userInput) {
        log.debug("Security check for tool='{}', userInput='{}'", toolName, userInput);

        // Step 1: Injection detection on user input
        InjectionResult injectionResult = InjectionResult.clean();
        if (userInput != null && !userInput.isBlank()) {
            injectionResult = injectionDetector.detect(userInput);
            if (injectionResult.isDetected()) {
                log.error("Prompt injection detected: pattern='{}', confidence={}",
                    injectionResult.getPattern(), injectionResult.getConfidence());
                return SecurityCheckResult.blocked(
                    "Prompt injection detected: " + injectionResult.getDescription(),
                    null, injectionResult);
            }
        }

        // Step 2: Risk assessment for the tool call
        RiskAssessment riskAssessment = riskScorer.assess(toolName, arguments);
        log.debug("Risk assessment: level={}, score={}, reason={}",
            riskAssessment.getLevel(), riskAssessment.getScore(), riskAssessment.getReason());

        // Step 3: Determine if sandbox is needed (for bash commands)
        boolean useSandbox = "bash".equals(toolName);

        // Step 4: Human escalation decision
        // Default agent confidence is 0.5 (neutral)
        double agentConfidence = 0.5;
        EscalationDecision escalationDecision = humanEscalation.shouldEscalate(riskAssessment, agentConfidence);

        if (escalationDecision.getLevel() == EscalationLevel.BLOCK) {
            log.warn("Operation blocked by escalation policy: {}", escalationDecision.getMessage());
            return SecurityCheckResult.needsApproval(
                escalationDecision.getMessage(),
                riskAssessment, injectionResult, escalationDecision);
        }

        String message = buildApprovalMessage(riskAssessment, escalationDecision);

        if (useSandbox) {
            return SecurityCheckResult.useSandbox(message, riskAssessment, injectionResult, escalationDecision);
        }

        return SecurityCheckResult.approved(message, riskAssessment, injectionResult, escalationDecision);
    }

    public String sanitizeOutput(String output) {
        return outputSanitizer.sanitize(output);
    }

    public SandboxExecutor getSandboxExecutor() {
        return sandboxExecutor;
    }

    public HumanEscalation getHumanEscalation() {
        return humanEscalation;
    }

    private String buildApprovalMessage(RiskAssessment risk, EscalationDecision escalation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Risk level: ").append(risk.getLevel());
        sb.append(" (score: ").append(String.format("%.1f", risk.getScore())).append(")");
        if (escalation.getMessage() != null && !escalation.getMessage().isEmpty()) {
            sb.append(" | ").append(escalation.getMessage());
        }
        return sb.toString();
    }
}
