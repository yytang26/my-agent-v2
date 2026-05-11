package com.agent.planner;

import com.agent.core.AgentResponse;
import com.agent.subagent.AgentOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 任务执行器 - 按依赖顺序并行执行任务
 */
@Component
public class TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutor.class);

    private final AgentOrchestrator agentOrchestrator;
    private final TaskPlanner taskPlanner;
    private final Replanner replanner;
    private final ExecutorService executor;

    @Value("${planner.max-retries:2}")
    private int maxRetries;

    @Value("${planner.parallel-threads:8}")
    private int parallelThreads;

    public TaskExecutor(AgentOrchestrator agentOrchestrator,
                        TaskPlanner taskPlanner,
                        Replanner replanner) {
        this.agentOrchestrator = agentOrchestrator;
        this.taskPlanner = taskPlanner;
        this.replanner = replanner;
        this.executor = Executors.newFixedThreadPool(parallelThreads);
    }

    /**
     * 执行整个 DAG，按依赖顺序并行执行就绪任务
     */
    public TaskDAG execute(TaskDAG dag) {
        log.info("[TaskExecutor] 开始执行 DAG，共 {} 个任务", dag.getTasks().size());

        int iteration = 0;
        final int maxIterations = dag.getTasks().size() * (maxRetries + 1) + 10;

        while (!dag.isComplete() && iteration < maxIterations) {
            iteration++;
            log.info("[TaskExecutor] === 执行迭代 #{} ===", iteration);

            List<PlanTask> readyTasks = dag.getReadyTasks();
            if (readyTasks.isEmpty()) {
                log.info("[TaskExecutor] 没有就绪任务，检查是否完成或失败");
                if (dag.hasFailed()) {
                    log.warn("[TaskExecutor] 检测到失败任务，终止执行");
                    break;
                }
                // 所有非完成/取消的任务都有未满足的依赖，但无环 DAG 不应出现这种情况
                if (!dag.isComplete()) {
                    log.warn("[TaskExecutor] 存在无法执行的任务，可能 DAG 有问题");
                    markRemainingBlocked(dag);
                }
                break;
            }

            log.info("[TaskExecutor] 并行执行 {} 个就绪任务", readyTasks.size());
            executeReadyTasksInParallel(dag, readyTasks);

            // 检查失败任务并尝试重规划
            if (dag.hasFailed()) {
                List<PlanTask> failedTasks = dag.getFailedTasks();
                log.warn("[TaskExecutor] 有 {} 个任务失败，尝试重规划", failedTasks.size());

                for (PlanTask failedTask : failedTasks) {
                    if (failedTask.getRetryCount() < maxRetries) {
                        log.info("[TaskExecutor] 对失败任务 {} 进行重规划 (第 {} 次重试)",
                                failedTask.getId(), failedTask.getRetryCount() + 1);
                        dag = replanner.replan(dag, failedTask);
                    } else {
                        log.error("[TaskExecutor] 任务 {} 已超过最大重试次数 ({})，标记为最终失败",
                                failedTask.getId(), maxRetries);
                        failedTask.setState(TaskState.FAILED);
                    }
                }
            }
        }

        if (iteration >= maxIterations) {
            log.error("[TaskExecutor] 达到最大迭代次数限制，强制终止");
        }

        log.info("[TaskExecutor] DAG 执行结束，完成={}, 失败={}", dag.isComplete(), dag.hasFailed());
        return dag;
    }

    /**
     * 并行执行一组就绪任务
     */
    private void executeReadyTasksInParallel(TaskDAG dag, List<PlanTask> readyTasks) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (PlanTask task : readyTasks) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                executeSingleTask(dag, task);
            }, executor);
            futures.add(future);
        }

        // 等待所有就绪任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * 执行单个任务
     */
    private void executeSingleTask(TaskDAG dag, PlanTask task) {
        task.setState(TaskState.IN_PROGRESS);
        log.info("[TaskExecutor] 开始执行任务 {}: {}", task.getId(), task.getDescription());

        try {
            // 通过 SubAgent 执行任务
            String systemPrompt = """
                    你是一个专注执行特定子任务的智能助手。
                    请只专注于完成给定的子任务，不要引入其他无关内容。
                    给出简洁明确的结果，不需要额外解释。
                    """;

            AgentResponse response = agentOrchestrator.delegate(task.getDescription(), systemPrompt);
            String result = response.getFinalMessage();

            task.setResult(result);
            task.setState(TaskState.COMPLETED);
            log.info("[TaskExecutor] 任务 {} 完成，迭代次数: {}", task.getId(), response.getTotalIterations());

        } catch (Exception e) {
            log.error("[TaskExecutor] 任务 {} 执行失败: {}", task.getId(), e.getMessage(), e);
            task.setError(e.getMessage());
            task.setRetryCount(task.getRetryCount() + 1);
            task.setState(TaskState.FAILED);
        }
    }

    /**
     * 将剩余未完成任务标记为 BLOCKED
     */
    private void markRemainingBlocked(TaskDAG dag) {
        for (PlanTask task : dag.getTasks().values()) {
            if (task.getState() == TaskState.PENDING) {
                task.setState(TaskState.BLOCKED);
            }
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
