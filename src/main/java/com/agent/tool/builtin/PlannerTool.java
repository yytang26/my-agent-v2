package com.agent.tool.builtin;

import com.agent.planner.TaskDAG;
import com.agent.planner.TaskExecutor;
import com.agent.planner.TaskPlanner;
import com.agent.tool.Tool;
import com.agent.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 规划工具 - 将复杂任务分解为子任务并执行
 */
@Component
public class PlannerTool {

    private static final Logger log = LoggerFactory.getLogger(PlannerTool.class);

    private final TaskPlanner taskPlanner;
    private final TaskExecutor taskExecutor;

    public PlannerTool(TaskPlanner taskPlanner, TaskExecutor taskExecutor) {
        this.taskPlanner = taskPlanner;
        this.taskExecutor = taskExecutor;
    }

    @Tool(name = "plan_task", description = "将复杂任务分解为子任务并执行")
    public String planTask(
            @ToolParam(name = "task", description = "复杂任务描述") String task
    ) {
        log.info("[PlannerTool] 收到复杂任务: {}", task);

        // 1. 规划任务
        TaskDAG dag = taskPlanner.plan(task);
        log.info("[PlannerTool] 任务分解完成，共 {} 个子任务", dag.getTasks().size());

        // 2. 执行 DAG
        TaskDAG resultDag = taskExecutor.execute(dag);
        log.info("[PlannerTool] DAG 执行完成");

        // 3. 汇总结果
        return buildResultSummary(resultDag);
    }

    /**
     * 构建执行结果摘要
     */
    private String buildResultSummary(TaskDAG dag) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 任务执行结果 ===\n\n");
        sb.append(dag.getSummary()).append("\n\n");

        // 汇总最终结果
        if (dag.isComplete()) {
            sb.append("\n=== 最终结果汇总 ===\n");
            for (var task : dag.getTasks().values()) {
                if (task.getResult() != null) {
                    sb.append(String.format("[%s] %s\n", task.getId(), task.getResult())).append("\n");
                }
            }
        }

        if (dag.hasFailed()) {
            sb.append("\n注意: 部分任务执行失败，请查看上方失败任务详情。\n");
        }

        return sb.toString().trim();
    }
}
