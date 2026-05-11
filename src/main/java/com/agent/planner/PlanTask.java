package com.agent.planner;

import java.util.List;
import java.util.Map;

public class PlanTask {

    private String id;
    private String description;
    private TaskState state;
    private List<String> dependsOn;
    private String result;
    private String error;
    private int retryCount;

    public PlanTask() {
        this.state = TaskState.PENDING;
        this.retryCount = 0;
    }

    public PlanTask(String id, String description, List<String> dependsOn) {
        this.id = id;
        this.description = description;
        this.dependsOn = dependsOn != null ? dependsOn : List.of();
        this.state = TaskState.PENDING;
        this.retryCount = 0;
    }

    /**
     * 判断该任务是否准备就绪（所有依赖已完成）
     */
    public boolean isReady(Map<String, PlanTask> allTasks) {
        if (state != TaskState.PENDING) {
            return false;
        }
        if (dependsOn == null || dependsOn.isEmpty()) {
            return true;
        }
        for (String depId : dependsOn) {
            PlanTask dep = allTasks.get(depId);
            if (dep == null || dep.getState() != TaskState.COMPLETED) {
                return false;
            }
        }
        return true;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    @Override
    public String toString() {
        return String.format("PlanTask{id='%s', desc='%s', state=%s, dependsOn=%s, retryCount=%d}",
                id, description, state, dependsOn, retryCount);
    }
}
