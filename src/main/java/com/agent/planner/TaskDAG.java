package com.agent.planner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 有向无环图表示任务依赖关系
 */
public class TaskDAG {

    private static final Logger log = LoggerFactory.getLogger(TaskDAG.class);

    private final Map<String, PlanTask> tasks;
    private List<String> executionOrder;

    public TaskDAG() {
        this.tasks = new LinkedHashMap<>();
        this.executionOrder = new ArrayList<>();
    }

    public void addTask(PlanTask task) {
        tasks.put(task.getId(), task);
        executionOrder = null; // invalidate cache
    }

    public PlanTask getTask(String id) {
        return tasks.get(id);
    }

    public Map<String, PlanTask> getTasks() {
        return Collections.unmodifiableMap(tasks);
    }

    /**
     * 获取所有依赖已满足的待执行任务
     */
    public List<PlanTask> getReadyTasks() {
        return tasks.values().stream()
                .filter(task -> task.isReady(tasks))
                .collect(Collectors.toList());
    }

    /**
     * 判断所有任务是否都已完成
     */
    public boolean isComplete() {
        return tasks.values().stream()
                .allMatch(t -> t.getState() == TaskState.COMPLETED
                        || t.getState() == TaskState.CANCELLED);
    }

    /**
     * 判断是否有任务失败
     */
    public boolean hasFailed() {
        return tasks.values().stream()
                .anyMatch(t -> t.getState() == TaskState.FAILED);
    }

    /**
     * 获取失败的任务列表
     */
    public List<PlanTask> getFailedTasks() {
        return tasks.values().stream()
                .filter(t -> t.getState() == TaskState.FAILED)
                .collect(Collectors.toList());
    }

    /**
     * 拓扑排序检测环
     * @throws IllegalStateException 如果存在环
     */
    public void validateNoCycle() {
        topologicalSort();
    }

    /**
     * 拓扑排序 (Kahn's algorithm)
     */
    public List<PlanTask> topologicalSort() {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (String taskId : tasks.keySet()) {
            inDegree.put(taskId, 0);
            adjacency.put(taskId, new ArrayList<>());
        }

        for (PlanTask task : tasks.values()) {
            if (task.getDependsOn() != null) {
                for (String depId : task.getDependsOn()) {
                    if (!tasks.containsKey(depId)) {
                        throw new IllegalStateException(
                                "任务 " + task.getId() + " 依赖了不存在的任务: " + depId);
                    }
                    adjacency.get(depId).add(task.getId());
                    inDegree.merge(task.getId(), 1, Integer::sum);
                }
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<PlanTask> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sorted.add(tasks.get(current));

            for (String neighbor : adjacency.get(current)) {
                int newDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newDegree);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (sorted.size() != tasks.size()) {
            List<String> cycleNodes = tasks.keySet().stream()
                    .filter(id -> !sorted.stream().map(PlanTask::getId).collect(Collectors.toSet()).contains(id))
                    .collect(Collectors.toList());
            throw new IllegalStateException("检测到循环依赖，涉及任务: " + cycleNodes);
        }

        this.executionOrder = sorted.stream().map(PlanTask::getId).collect(Collectors.toList());
        log.debug("拓扑排序结果: {}", executionOrder);
        return sorted;
    }

    /**
     * 获取执行顺序
     */
    public List<String> getExecutionOrder() {
        if (executionOrder == null) {
            topologicalSort();
        }
        return Collections.unmodifiableList(executionOrder);
    }

    /**
     * 生成 DAG 的可读摘要
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("任务规划 DAG (共 ").append(tasks.size()).append(" 个任务):\n");

        long completed = tasks.values().stream().filter(t -> t.getState() == TaskState.COMPLETED).count();
        long failed = tasks.values().stream().filter(t -> t.getState() == TaskState.FAILED).count();
        long pending = tasks.values().stream().filter(t -> t.getState() == TaskState.PENDING).count();
        long inProgress = tasks.values().stream().filter(t -> t.getState() == TaskState.IN_PROGRESS).count();

        sb.append(String.format("  状态: 完成=%d, 失败=%d, 执行中=%d, 待执行=%d%n", completed, failed, inProgress, pending));
        sb.append("\n任务列表:\n");

        for (PlanTask task : tasks.values()) {
            String deps = task.getDependsOn() != null && !task.getDependsOn().isEmpty()
                    ? " (依赖: " + String.join(", ", task.getDependsOn()) + ")"
                    : "";
            String result = task.getResult() != null ? " -> " + truncate(task.getResult(), 80) : "";
            String error = task.getError() != null ? " [错误: " + truncate(task.getError(), 60) + "]" : "";
            sb.append(String.format("  [%s] %s%s%s%s%n",
                    task.getState(), task.getDescription(), deps, result, error));
        }

        return sb.toString().trim();
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        String oneLine = s.replace("\n", " ").trim();
        return oneLine.length() > maxLen ? oneLine.substring(0, maxLen) + "..." : oneLine;
    }
}
