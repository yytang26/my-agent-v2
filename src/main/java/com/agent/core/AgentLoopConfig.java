package com.agent.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgentLoopConfig {

    @Value("${agent.loop.max-iterations:10}")
    private int maxIterations = 10;

    @Value("${agent.loop.parallel-tool-execution:true}")
    private boolean parallelToolExecution = true;

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public boolean isParallelToolExecution() {
        return parallelToolExecution;
    }

    public void setParallelToolExecution(boolean parallelToolExecution) {
        this.parallelToolExecution = parallelToolExecution;
    }
}
