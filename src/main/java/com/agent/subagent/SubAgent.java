package com.agent.subagent;

import com.agent.core.AgentResponse;
import com.agent.core.AgentState;
import com.agent.core.TurnResult;
import com.agent.llm.LlmClient;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ContentBlock;
import com.agent.llm.model.ModelConfig;
import com.agent.memory.ConversationMemory;
import com.agent.memory.InMemoryConversationMemory;
import com.agent.memory.Message;
import com.agent.memory.MessageRole;
import com.agent.tool.ToolDefinition;
import com.agent.tool.ToolExecutor;
import com.agent.tool.ToolRegistry;
import com.agent.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SubAgent {

    private static final Logger log = LoggerFactory.getLogger(SubAgent.class);

    private final String id;
    private final String name;
    private final ConversationMemory memory;
    private final String systemPrompt;
    private final LlmClient llmClient;
    private final ToolExecutor toolExecutor;
    private final ToolRegistry toolRegistry;
    private final int maxIterations;

    public SubAgent(String name, String systemPrompt,
                    LlmClient llmClient, ToolExecutor toolExecutor,
                    ToolRegistry toolRegistry, int maxIterations) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.memory = new InMemoryConversationMemory();
        this.systemPrompt = systemPrompt;
        this.llmClient = llmClient;
        this.toolExecutor = toolExecutor;
        this.toolRegistry = toolRegistry;
        this.maxIterations = maxIterations;

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            this.memory.setSystemPrompt(systemPrompt);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ConversationMemory getMemory() {
        return memory;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public AgentResponse run(String task) {
        log.info("[SubAgent:{}] 开始执行任务: {}", name, task);
        memory.addMessage(Message.user(task));

        List<TurnResult> turns = new ArrayList<>();
        List<ToolDefinition> tools = toolRegistry.getAllToolDefinitions();
        ModelConfig modelConfig = ModelConfig.builder().maxTokens(4096).build();

        int iteration = 0;
        while (iteration < maxIterations) {
            iteration++;
            log.info("[SubAgent:{}] 第 {} 轮迭代", name, iteration);

            List<ChatMessage> chatMessages = buildChatMessages();
            ChatResponse response;
            try {
                response = llmClient.chat(chatMessages, modelConfig, tools);
            } catch (Exception e) {
                log.error("[SubAgent:{}] LLM 调用失败: {}", name, e.getMessage());
                String errorMsg = "子Agent执行出错: " + e.getMessage();
                turns.add(new TurnResult(AgentState.RESPONDING, errorMsg, List.of(), List.of(), iteration));
                return new AgentResponse(errorMsg, turns, iteration, false);
            }

            if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                String errorMsg = "子Agent未收到有效回复。";
                turns.add(new TurnResult(AgentState.RESPONDING, errorMsg, List.of(), List.of(), iteration));
                return new AgentResponse(errorMsg, turns, iteration, false);
            }

            if (response.hasToolUse()) {
                List<ContentBlock> toolUseBlocks = response.getToolUseBlocks();
                List<TurnResult.ToolCall> toolCalls = new ArrayList<>();
                List<ToolResult> toolResults = new ArrayList<>();

                // 构建 assistant 消息（含 tool_use）
                memory.addChatMessage(ChatMessage.assistant(response.getContent()));

                for (ContentBlock block : toolUseBlocks) {
                    String toolUseId = block.getId();
                    String toolName = block.getName();
                    Map<String, Object> arguments = block.getInput() != null ? block.getInput() : Map.of();

                    toolCalls.add(new TurnResult.ToolCall(toolUseId, toolName, arguments));
                    log.info("[SubAgent:{}] 执行工具: {} (id={}), 参数: {}", name, toolName, toolUseId, arguments);

                    ToolResult result = toolExecutor.execute(toolName, toolUseId, arguments);
                    toolResults.add(result);
                    log.info("[SubAgent:{}] 工具结果: {}", name, result);

                    memory.addChatMessage(ChatMessage.toolResult(toolUseId, result.getContent(), result.isError()));
                }

                String assistantText = response.getFirstTextContent();
                if (assistantText == null) {
                    assistantText = "[调用工具]";
                }
                turns.add(new TurnResult(AgentState.CALLING_TOOL, assistantText, toolCalls, toolResults, iteration));
            } else {
                String finalText = response.getFirstTextContent();
                if (finalText == null) {
                    finalText = "(无文本内容)";
                }
                log.info("[SubAgent:{}] 生成最终回复", name);
                memory.addChatMessage(ChatMessage.assistant(finalText));
                turns.add(new TurnResult(AgentState.RESPONDING, finalText, List.of(), List.of(), iteration));
                return new AgentResponse(finalText, turns, iteration, false);
            }
        }

        log.warn("[SubAgent:{}] 达到最大迭代次数: {}", name, maxIterations);
        String maxIterMsg = "子Agent处理请求所需的步骤过多，已达到最大迭代限制。";
        turns.add(new TurnResult(AgentState.RESPONDING, maxIterMsg, List.of(), List.of(), iteration));
        return new AgentResponse(maxIterMsg, turns, iteration, true);
    }

    private List<ChatMessage> buildChatMessages() {
        List<ChatMessage> result = new ArrayList<>();
        for (Message msg : memory.getMessages()) {
            switch (msg.role()) {
                case SYSTEM -> result.add(new ChatMessage("system", msg.content()));
                case USER -> result.add(ChatMessage.user(msg.content()));
                case ASSISTANT -> result.add(ChatMessage.assistant(msg.content()));
                case TOOL -> result.add(ChatMessage.user(msg.content()));
            }
        }
        return result;
    }
}
