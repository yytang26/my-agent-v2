package com.agent.planner;

import com.agent.llm.LlmClient;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 任务规划器 - 将复杂任务分解为子任务并构建 DAG
 */
@Component
public class TaskPlanner {

    private static final Logger log = LoggerFactory.getLogger(TaskPlanner.class);

    private final LlmClient llmClient;

    @Value("${planner.mock:false}")
    private boolean mockMode;

    public TaskPlanner(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * 将复杂任务分解为子任务 DAG
     */
    public TaskDAG plan(String complexTask) {
        log.info("[TaskPlanner] 开始规划任务: {}", complexTask);

        TaskDAG dag;
        if (mockMode) {
            dag = planMock(complexTask);
        } else {
            dag = planWithLlm(complexTask);
        }

        // 验证 DAG 无环
        dag.validateNoCycle();
        log.info("[TaskPlanner] 规划完成，共 {} 个子任务", dag.getTasks().size());
        return dag;
    }

    /**
     * LLM 驱动的任务分解
     */
    private TaskDAG planWithLlm(String complexTask) {
        String prompt = buildDecompositionPrompt(complexTask);

        try {
            String response = llmClient.ask(prompt);
            log.debug("[TaskPlanner] LLM 原始回复:\n{}", response);
            return parseLlmResponse(response);
        } catch (Exception e) {
            log.warn("[TaskPlanner] LLM 分解失败，回退到 Mock 模式: {}", e.getMessage());
            return planMock(complexTask);
        }
    }

    private String buildDecompositionPrompt(String task) {
        return """
                你是一个任务规划专家。请将以下复杂任务分解为多个独立的子任务，并指定执行依赖关系。

                复杂任务: %s

                请严格按照以下 JSON 格式输出，不要包含其他内容:
                [
                  {"id": "task-1", "description": "子任务描述", "dependsOn": []},
                  {"id": "task-2", "description": "子任务描述", "dependsOn": ["task-1"]},
                  {"id": "task-3", "description": "子任务描述", "dependsOn": ["task-1"]}
                ]

                要求:
                1. 每个子任务应该是一个可独立执行的原子操作
                2. dependsOn 列表指定该任务依赖的前置任务 id
                3. 不存在循环依赖
                4. 所有任务的 id 必须唯一
                5. 尽量使子任务之间可以并行执行（减少不必要的依赖）
                """.formatted(task);
    }

    private TaskDAG parseLlmResponse(String response) {
        TaskDAG dag = new TaskDAG();

        // 尝试提取 JSON 数组
        String json = extractJsonArray(response);
        if (json == null || json.isBlank()) {
            log.warn("[TaskPlanner] 无法从 LLM 回复中提取 JSON，回退到 Mock 模式");
            throw new RuntimeException("无法解析 LLM 回复为 JSON");
        }

        // 简单解析 JSON（不依赖 Jackson 的复杂解析，手动提取字段）
        List<Map<String, Object>> taskMaps = parseSimpleJsonArray(json);

        for (Map<String, Object> taskMap : taskMaps) {
            String id = (String) taskMap.get("id");
            String description = (String) taskMap.get("description");
            @SuppressWarnings("unchecked")
            List<String> dependsOn = (List<String>) taskMap.getOrDefault("dependsOn", List.of());

            if (id == null || description == null) {
                log.warn("[TaskPlanner] 跳过无效任务条目: {}", taskMap);
                continue;
            }

            PlanTask task = new PlanTask(id, description, dependsOn);
            dag.addTask(task);
        }

        if (dag.getTasks().isEmpty()) {
            throw new RuntimeException("LLM 未生成有效的子任务");
        }

        return dag;
    }

    /**
     * 从文本中提取 JSON 数组
     */
    private String extractJsonArray(String text) {
        // 尝试找到 [ ... ] 模式
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    /**
     * 简单 JSON 数组解析（避免复杂依赖）
     */
    private List<Map<String, Object>> parseSimpleJsonArray(String json) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 使用正则提取每个 { ... } 对象
        Pattern objectPattern = Pattern.compile("\\{[^{}]*\\}");
        Matcher matcher = objectPattern.matcher(json);

        while (matcher.find()) {
            String obj = matcher.group();
            Map<String, Object> map = new HashMap<>();

            // 提取 "id": "value"
            Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
            Matcher idMatcher = idPattern.matcher(obj);
            if (idMatcher.find()) {
                map.put("id", idMatcher.group(1));
            }

            // 提取 "description": "value"
            Pattern descPattern = Pattern.compile("\"description\"\\s*:\\s*\"([^\"]+)\"");
            Matcher descMatcher = descPattern.matcher(obj);
            if (descMatcher.find()) {
                map.put("description", descMatcher.group(1));
            }

            // 提取 "dependsOn": ["task-1", "task-2"]
            Pattern depsPattern = Pattern.compile("\"dependsOn\"\\s*:\\s*\\[([^]]*)\\]");
            Matcher depsMatcher = depsPattern.matcher(obj);
            if (depsMatcher.find()) {
                String depsStr = depsMatcher.group(1).trim();
                List<String> deps = new ArrayList<>();
                if (!depsStr.isEmpty()) {
                    Pattern depIdPattern = Pattern.compile("\"([^\"]+)\"");
                    Matcher depIdMatcher = depIdPattern.matcher(depsStr);
                    while (depIdMatcher.find()) {
                        deps.add(depIdMatcher.group(1));
                    }
                }
                map.put("dependsOn", deps);
            }

            result.add(map);
        }

        return result;
    }

    /**
     * Mock 模式 - 简单地将任务分为 2-3 个子任务
     */
    private TaskDAG planMock(String complexTask) {
        log.info("[TaskPlanner] Mock 模式：简单分解任务");
        TaskDAG dag = new TaskDAG();

        dag.addTask(new PlanTask("task-1", "分析任务: " + complexTask, List.of()));
        dag.addTask(new PlanTask("task-2", "执行核心步骤: " + complexTask, List.of("task-1")));
        dag.addTask(new PlanTask("task-3", "验证结果: " + complexTask, List.of("task-2")));

        return dag;
    }
}
