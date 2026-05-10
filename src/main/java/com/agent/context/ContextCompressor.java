package com.agent.context;

import com.agent.memory.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ContextCompressor {

    private static final Logger log = LoggerFactory.getLogger(ContextCompressor.class);

    private final TokenEstimator tokenEstimator;
    private final TieredCompression tieredCompression;

    public ContextCompressor(TokenEstimator tokenEstimator, TieredCompression tieredCompression) {
        this.tokenEstimator = tokenEstimator;
        this.tieredCompression = tieredCompression;
    }

    public List<Message> compressIfNeeded(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        int before = tokenEstimator.estimate(messages);
        TieredCompression.CompressionPlan plan = tieredCompression.decide(before);

        if (plan.getLevel() == 0) {
            return new ArrayList<>(messages);
        }

        List<Message> result = new ArrayList<>(messages);
        List<String> appliedStrategies = new ArrayList<>();

        for (CompressionStrategy strategy : plan.getStrategies()) {
            result = strategy.compress(result, plan.getTargetTokens());
            appliedStrategies.add(strategy.name());
        }

        int after = tokenEstimator.estimate(result);
        log.info("上下文压缩: {} tokens -> {} tokens (策略: {})", before, after, String.join(" + ", appliedStrategies));

        return result;
    }
}
