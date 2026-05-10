package com.agent.core;

import java.util.List;

public class AgentResponse {

    private String finalMessage;
    private List<TurnResult> turns;
    private int totalIterations;
    private boolean reachedMaxIterations;

    public AgentResponse(String finalMessage, List<TurnResult> turns,
                         int totalIterations, boolean reachedMaxIterations) {
        this.finalMessage = finalMessage;
        this.turns = turns;
        this.totalIterations = totalIterations;
        this.reachedMaxIterations = reachedMaxIterations;
    }

    public String getFinalMessage() {
        return finalMessage;
    }

    public void setFinalMessage(String finalMessage) {
        this.finalMessage = finalMessage;
    }

    public List<TurnResult> getTurns() {
        return turns;
    }

    public void setTurns(List<TurnResult> turns) {
        this.turns = turns;
    }

    public int getTotalIterations() {
        return totalIterations;
    }

    public void setTotalIterations(int totalIterations) {
        this.totalIterations = totalIterations;
    }

    public boolean isReachedMaxIterations() {
        return reachedMaxIterations;
    }

    public void setReachedMaxIterations(boolean reachedMaxIterations) {
        this.reachedMaxIterations = reachedMaxIterations;
    }

    @Override
    public String toString() {
        return "AgentResponse{finalMessage='" + finalMessage + '\'' +
                ", totalIterations=" + totalIterations +
                ", reachedMaxIterations=" + reachedMaxIterations +
                ", turns=" + turns +
                '}';
    }
}
