package com.agent.eval;

import com.agent.core.AgentLoop;
import com.agent.core.AgentResponse;
import com.agent.core.TurnResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent 评估器
 * 对测试用例运行 AgentLoop 并检查结果
 */
@Component
public class AgentEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AgentEvaluator.class);

    private final AgentLoop agentLoop;
    private final ObjectMapper objectMapper;

    private volatile EvalReport lastReport;

    public AgentEvaluator(AgentLoop agentLoop) {
        this.agentLoop = agentLoop;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 加载默认测试用例
     */
    public List<EvalTestCase> loadDefaultTestCases() {
        try {
            ClassPathResource resource = new ClassPathResource("eval/test-cases.json");
            try (InputStream is = resource.getInputStream()) {
                return objectMapper.readValue(is, new TypeReference<List<EvalTestCase>>() {});
            }
        } catch (Exception e) {
            log.error("[AgentEvaluator] 加载测试用例失败", e);
            return List.of();
        }
    }

    /**
     * 运行评估
     */
    public EvalReport evaluate(List<EvalTestCase> testCases) {
        log.info("[AgentEvaluator] 开始评估，共 {} 个测试用例", testCases.size());

        List<EvalResult> results = new ArrayList<>();
        int passed = 0;
        int failed = 0;

        for (EvalTestCase testCase : testCases) {
            log.info("[AgentEvaluator] 运行测试 #{}: {}", testCase.getId(), testCase.getInput());
            EvalResult result = runTestCase(testCase);
            results.add(result);

            if (result.isPassed()) {
                passed++;
            } else {
                failed++;
            }
        }

        int total = testCases.size();
        double successRate = total > 0 ? (double) passed / total : 0;

        EvalReport report = new EvalReport(total, passed, failed, successRate, results);
        lastReport = report;

        log.info("[AgentEvaluator] 评估完成: {}/{} 通过 ({})",
                passed, total, String.format("%.1f%%", successRate * 100));

        return report;
    }

    private EvalResult runTestCase(EvalTestCase testCase) {
        try {
            AgentResponse response = agentLoop.run(testCase.getInput());

            // 收集调用的工具
            List<String> toolsCalled = response.getTurns().stream()
                    .filter(t -> t.getToolCalls() != null)
                    .flatMap(t -> t.getToolCalls().stream())
                    .map(TurnResult.ToolCall::name)
                    .collect(Collectors.toList());

            String output = response.getFinalMessage();

            // 检查 1: 是否调用了预期工具
            if (testCase.getRequiredTools() != null && !testCase.getRequiredTools().isEmpty()) {
                for (String requiredTool : testCase.getRequiredTools()) {
                    boolean found = toolsCalled.stream()
                            .anyMatch(t -> t.equalsIgnoreCase(requiredTool));
                    if (!found) {
                        return new EvalResult(
                                testCase.getId(),
                                false,
                                output,
                                toolsCalled,
                                "期望调用工具 " + requiredTool + "，但实际未调用"
                        );
                    }
                }
            }

            // 检查 2: 不应调用工具的测试用例
            if (testCase.getRequiredTools() != null && testCase.getRequiredTools().isEmpty()) {
                if (!toolsCalled.isEmpty()) {
                    return new EvalResult(
                            testCase.getId(),
                            false,
                            output,
                            toolsCalled,
                            "期望不调用工具，但实际调用了: " + toolsCalled
                    );
                }
            }

            // 检查 3: 输出不为空
            if (output == null || output.isBlank()) {
                return new EvalResult(
                        testCase.getId(),
                        false,
                        output,
                        toolsCalled,
                        "输出为空"
                );
            }

            return new EvalResult(testCase.getId(), true, output, toolsCalled, null);

        } catch (Exception e) {
            log.error("[AgentEvaluator] 测试 #{} 执行异常", testCase.getId(), e);
            return new EvalResult(
                    testCase.getId(),
                    false,
                    null,
                    List.of(),
                    "执行异常: " + e.getMessage()
            );
        }
    }

    /**
     * 获取最近一次评估报告
     */
    public EvalReport getLastReport() {
        return lastReport;
    }
}
