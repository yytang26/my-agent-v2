package com.agent.context;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TieredCompression {

    @Value("${agent.context.window-size:200000}")
    private int contextWindowSize;

    @Value("${agent.context.compression.level1-threshold:0.7}")
    private double level1Threshold;

    @Value("${agent.context.compression.level2-threshold:0.85}")
    private double level2Threshold;

    @Value("${agent.context.compression.level3-threshold:0.95}")
    private double level3Threshold;

    private final ToolResultPruner toolResultPruner;
    private final SlidingWindowCompressor slidingWindowCompressor;
    private final SummaryCompressor summaryCompressor;

    public TieredCompression(ToolResultPruner toolResultPruner,
                             SlidingWindowCompressor slidingWindowCompressor,
                             SummaryCompressor summaryCompressor) {
        this.toolResultPruner = toolResultPruner;
        this.slidingWindowCompressor = slidingWindowCompressor;
        this.summaryCompressor = summaryCompressor;
    }

    public CompressionPlan decide(int currentTokens) {
        double ratio = (double) currentTokens / contextWindowSize;

        if (ratio < level1Threshold) {
            return new CompressionPlan(0, List.of(), 0);
        } else if (ratio < level2Threshold) {
            return new CompressionPlan(1, List.of(toolResultPruner), 0);
        } else if (ratio < level3Threshold) {
            int target = (int) (contextWindowSize * level1Threshold);
            return new CompressionPlan(2, List.of(toolResultPruner, slidingWindowCompressor), target);
        } else {
            int target = (int) (contextWindowSize * level1Threshold);
            return new CompressionPlan(3, List.of(summaryCompressor), target);
        }
    }

    public int getContextWindowSize() {
        return contextWindowSize;
    }

    public double getLevel1Threshold() {
        return level1Threshold;
    }

    public double getLevel2Threshold() {
        return level2Threshold;
    }

    public double getLevel3Threshold() {
        return level3Threshold;
    }

    public static class CompressionPlan {
        private final int level;
        private final List<CompressionStrategy> strategies;
        private final int targetTokens;

        public CompressionPlan(int level, List<CompressionStrategy> strategies, int targetTokens) {
            this.level = level;
            this.strategies = strategies;
            this.targetTokens = targetTokens;
        }

        public int getLevel() {
            return level;
        }

        public List<CompressionStrategy> getStrategies() {
            return strategies;
        }

        public int getTargetTokens() {
            return targetTokens;
        }
    }
}
