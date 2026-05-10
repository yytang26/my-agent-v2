package com.agent.context;

import com.agent.memory.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PreflightTokenCheck {

    private final TokenEstimator tokenEstimator;

    public PreflightTokenCheck(TokenEstimator tokenEstimator) {
        this.tokenEstimator = tokenEstimator;
    }

    public boolean willOverflow(List<Message> messages, int contextWindowSize) {
        int total = estimateTotal(messages);
        return total > (int) (contextWindowSize * 0.95);
    }

    public int estimateTotal(List<Message> messages) {
        return tokenEstimator.estimate(messages);
    }
}
