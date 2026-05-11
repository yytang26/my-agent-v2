package com.agent.security;

import com.agent.permission.InteractivePrompter;
import com.agent.permission.PermissionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HumanEscalation {

    private static final Logger log = LoggerFactory.getLogger(HumanEscalation.class);

    private final InteractivePrompter interactivePrompter;
    private final double riskThreshold;

    public HumanEscalation(InteractivePrompter interactivePrompter,
                           @Value("${agent.security.risk-threshold:0.7}") double riskThreshold) {
        this.interactivePrompter = interactivePrompter;
        this.riskThreshold = riskThreshold;
    }

    public EscalationDecision shouldEscalate(RiskAssessment risk, double agentConfidence) {
        if (risk == null) {
            return EscalationDecision.warn("Unable to assess risk, defaulting to warning");
        }

        return switch (risk.getLevel()) {
            case CRITICAL -> {
                log.warn("CRITICAL risk operation requires human approval: {}", risk.getReason());
                yield EscalationDecision.block("CRITICAL risk: " + risk.getReason());
            }
            case HIGH -> {
                if (agentConfidence < 0.5) {
                    log.warn("HIGH risk + low confidence ({}), requiring human approval: {}", agentConfidence, risk.getReason());
                    yield EscalationDecision.block("HIGH risk with low confidence (" + String.format("%.2f", agentConfidence) + "): " + risk.getReason());
                }
                log.warn("HIGH risk operation, warning only: {}", risk.getReason());
                yield EscalationDecision.warn("HIGH risk: " + risk.getReason());
            }
            case MEDIUM -> {
                log.info("MEDIUM risk operation,提示但不阻塞: {}", risk.getReason());
                yield EscalationDecision.warn("MEDIUM risk: " + risk.getReason());
            }
            case LOW -> {
                log.debug("LOW risk operation, auto-approved: {}", risk.getReason());
                yield EscalationDecision.autoPass();
            }
        };
    }

    public boolean requestHumanApproval(String description) {
        if (interactivePrompter == null) {
            log.warn("InteractivePrompter not available, defaulting to deny for: {}", description);
            return false;
        }

        log.info("Requesting human approval for: {}", description);

        // Create a temporary map for the prompter
        Map<String, Object> args = Map.of(
            "description", description,
            "requires_approval", "true"
        );

        PermissionResponse response = interactivePrompter.prompt("security_approval", args);

        boolean approved = response == PermissionResponse.YES || response == PermissionResponse.ALWAYS;
        log.info("Human approval result for '{}': {}", description, approved ? "APPROVED" : "DENIED");
        return approved;
    }
}
