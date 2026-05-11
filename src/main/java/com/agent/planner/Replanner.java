package com.agent.planner;

import com.agent.llm.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 重规划器 - 任务失败后重新规划剩余任务
 */
@Component
public class Replanner {

    private static final Logger log = LoggerFactory.getLogger(Replanner.class);

    private final LlmClient llmClient;

    @Value("${planner.mock:false}")
    private boolean mockMode;

    @Value("${planner.max-retries:2}")
    private int maxRetries;

    public Replanner(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * 重新规划：处理失败任务
     * @param dag 当前 DAG
     * @param failedTask 失败的任务
     * @return 更新后的 DAG
     */
    public TaskDAG replan(TaskDAG dag, PlanTask failedTask) {
        log.info("[Replanner] 开始重规划，失败任务: {} (错误: {})",
                failedTask.getId(), failedTask.getError());

        if (mockMode) {
            return replanMock(dag, failedTask);
        } else {
            return replanWithLlm(dag, failedTask);
        }
    }

    /**
     * LLM 驱动的重规划
     */
    private TaskDAG replanWithLlm(TaskDAG dag, PlanTask failedTask) {
        String prompt = buildReplanPrompt(dag, failedTask);

        try {
            String response = llmClient.ask(prompt);
            log.debug("[Replanner] LLM 重规划回复:\n{}", response);

            // 简单策略：如果 LLM 建议跳过，则标记为取消
            if (response.contains("跳过") || response.contains("skip")) {
                log.info("[Replanner] LLM 建议跳过失败任务 {}", failedTask.getId());
                failedTask.setState(TaskState.CANCELLED);
                return dag;
            }

            // 如果 LLM 建议替代方案，更新任务描述并重试
            if (response.contains("替代") || response.contains("alternative")) {
                String newDesc = extractAlternativeDescription(response);
                if (newDesc != null && !newDesc.isBlank()) {
                    log.info("[Replanner] LLM 建议替代方案: {}", newDesc);
                    failedTask.setDescription(newDesc);
                    failedTask.setState(TaskState.PENDING);
                    failedTask.setError(null);
                    return dag;
                }
            }

            // 默认策略：简单重试
            return replanMock(dag, failedTask);

        } catch (Exception e) {
            log.warn("[Replanner] LLM 重规划失败，回退到 Mock 模式: {}", e.getMessage());
            return replanMock(dag, failedTask);
        }
    }

    private String buildReplanPrompt(TaskDAG dag, PlanTask failedTask) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个任务重规划专家。当前任务执行计划中有一个任务失败了。\n\n");
        sb.append("失败任务: ").append(failedTask.getId()).append("\n");
        sb.append("任务描述: ").append(failedTask.getDescription()).append("\n");
        sb.append("失败原因: ").append(failedTask.getError()).append("\n");
        sb.append("当前重试次数: ").append(failedTask.getRetryCount()).append("/").append(maxRetries).append("\n\n");

        sb.append("当前 DAG 状态:\n");
        for (PlanTask task : dag.getTasks().values()) {
            sb.append("  - ").append(task.getId()).append(": ")
                    .append(task.getState()).append(" (").append(task.getDescription()).append(")\n");
        }

        sb.append("\n请给出处理建议（只需返回以下关键词之一）:\n");
        sb.append("- '重试'：直接重试失败任务\n");
        sb.append("- '跳过'：跳过失败任务，继续执行后续任务\n");
        sb.append("- '替代'：提供一个替代方案，在下一行给出替代描述\n");

        return sb.toString();
    }

    private String extractAlternativeDescription(String response) {
        // 尝试提取替代描述（在 "替代" 或 "alternative" 之后的一行）
        String[] lines = response.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("替代") || lines[i].contains("alternative")) {
                if (i + 1 < lines.length) {
                    String next = lines[i + 1].trim();
                    if (!next.isEmpty() && !next.startsWith("-")) {
                        return next;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Mock 模式 - 简单重试失败任务（最多 2 次）
     */
    private TaskDAG replanMock(TaskDAG dag, PlanTask failedTask) {
        log.info("[Replanner] Mock 模式：简单重试任务 {}", failedTask.getId());

        if (failedTask.getRetryCount() < maxRetries) {
            failedTask.setState(TaskState.PENDING);
            failedTask.setError(null);
            failedTask.setRetryCount(failedTask.getRetryCount());
            log.info("[Replanner] 重置任务 {} 为 PENDING 状态，准备重试", failedTask.getId());
        } else {
            failedTask.setState(TaskState.FAILED);
            log.warn("[Replanner] 任务 {} 已超过最大重试次数，保持失败状态", failedTask.getId());
        }

        return dag;
    }
}
