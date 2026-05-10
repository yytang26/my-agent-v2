package com.agent.tool.builtin;

import com.agent.core.AgentResponse;
import com.agent.subagent.AgentOrchestrator;
import com.agent.tool.Tool;
import com.agent.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SubAgentTool {

    private static final Logger log = LoggerFactory.getLogger(SubAgentTool.class);

    private final AgentOrchestrator agentOrchestrator;

    public SubAgentTool(AgentOrchestrator agentOrchestrator) {
        this.agentOrchestrator = agentOrchestrator;
    }

    @Tool(name = "delegate_task", description = "将子任务委派给一个独立的子Agent执行")
    public String delegateTask(
            @ToolParam(name = "task", description = "子任务描述") String task,
            @ToolParam(name = "context", description = "提供给子Agent的上下文", required = false) String context
    ) {
        log.info("[SubAgentTool] 收到委派任务: {}", task);

        String systemPrompt = """
                你是一个独立的子Agent，专注于完成被委派的特定任务。
                请使用可用的工具来完成任务，并给出简洁明确的最终结果。
                """;

        if (context != null && !context.isBlank()) {
            systemPrompt += "\n\n额外上下文:\n" + context;
        }

        AgentResponse response = agentOrchestrator.delegate(task, systemPrompt);
        String result = response.getFinalMessage();

        log.info("[SubAgentTool] 子Agent任务完成，迭代次数: {}", response.getTotalIterations());
        return result;
    }
}
