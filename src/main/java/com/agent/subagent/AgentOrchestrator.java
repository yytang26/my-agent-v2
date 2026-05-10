package com.agent.subagent;

import com.agent.core.AgentResponse;
import com.agent.llm.LlmClient;
import com.agent.tool.ToolExecutor;
import com.agent.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final LlmClient llmClient;
    private final ToolExecutor toolExecutor;
    private final ToolRegistry toolRegistry;

    @Value("${subagent.max-iterations:5}")
    private int defaultMaxIterations = 5;

    public AgentOrchestrator(LlmClient llmClient, ToolExecutor toolExecutor, ToolRegistry toolRegistry) {
        this.llmClient = llmClient;
        this.toolExecutor = toolExecutor;
        this.toolRegistry = toolRegistry;
    }

    public SubAgent createSubAgent(String name, String systemPrompt) {
        return new SubAgent(name, systemPrompt, llmClient, toolExecutor, toolRegistry, defaultMaxIterations);
    }

    public SubAgent createSubAgent(String name, String systemPrompt, int maxIterations) {
        return new SubAgent(name, systemPrompt, llmClient, toolExecutor, toolRegistry, maxIterations);
    }

    public AgentResponse delegate(String task, String systemPrompt) {
        SubAgent subAgent = createSubAgent("delegate-" + System.currentTimeMillis(), systemPrompt);
        log.info("[Orchestrator] 委派任务到子Agent: {}", task);
        return subAgent.run(task);
    }

    public AgentResponse delegate(String task, String systemPrompt, int maxIterations) {
        SubAgent subAgent = createSubAgent("delegate-" + System.currentTimeMillis(), systemPrompt, maxIterations);
        log.info("[Orchestrator] 委派任务到子Agent: {}", task);
        return subAgent.run(task);
    }

    public List<AgentResponse> delegateParallel(List<String> tasks) {
        return delegateParallel(tasks, "");
    }

    public List<AgentResponse> delegateParallel(List<String> tasks, String systemPrompt) {
        log.info("[Orchestrator] 并行委派 {} 个任务", tasks.size());

        List<CompletableFuture<AgentResponse>> futures = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            final int index = i;
            final String task = tasks.get(i);
            CompletableFuture<AgentResponse> future = CompletableFuture.supplyAsync(() -> {
                String agentName = "parallel-" + index;
                SubAgent subAgent = createSubAgent(agentName, systemPrompt);
                return subAgent.run(task);
            });
            futures.add(future);
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    public List<AgentResponse> delegateParallel(List<String> tasks, String systemPrompt, int maxIterations) {
        log.info("[Orchestrator] 并行委派 {} 个任务", tasks.size());

        List<CompletableFuture<AgentResponse>> futures = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            final int index = i;
            final String task = tasks.get(i);
            CompletableFuture<AgentResponse> future = CompletableFuture.supplyAsync(() -> {
                String agentName = "parallel-" + index;
                SubAgent subAgent = createSubAgent(agentName, systemPrompt, maxIterations);
                return subAgent.run(task);
            });
            futures.add(future);
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }
}
