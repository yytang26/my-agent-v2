package com.agent.core;

import com.agent.llm.LlmClient;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ContentBlock;
import com.agent.llm.model.ModelConfig;
import com.agent.memory.ConversationMemory;
import com.agent.memory.Message;
import com.agent.session.SessionManager;
import com.agent.tool.ToolDefinition;
import com.agent.tool.ToolExecutor;
import com.agent.tool.ToolRegistry;
import com.agent.tool.ToolResult;
import com.agent.tracking.TokenTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AgentLoop {

    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);

    private final LlmClient llmClient;
    private final ConversationMemory memory;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final TokenTracker tokenTracker;
    private final AgentLoopConfig config;
    private final SessionManager sessionManager;

    public AgentLoop(LlmClient llmClient, ConversationMemory memory,
                     ToolRegistry toolRegistry, ToolExecutor toolExecutor,
                     TokenTracker tokenTracker, AgentLoopConfig config,
                     SessionManager sessionManager) {
        this.llmClient = llmClient;
        this.memory = memory;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.tokenTracker = tokenTracker;
        this.config = config;
        this.sessionManager = sessionManager;
    }

    public AgentResponse run(String userMessage) {
        log.info("[AgentLoop] 开始处理用户消息: {}", userMessage);

        // 1. 将 userMessage 加入 memory
        memory.addMessage(Message.user(userMessage));

        List<TurnResult> turns = new ArrayList<>();
        int iteration = 0;

        List<ToolDefinition> tools = toolRegistry.getAllToolDefinitions();
        ModelConfig modelConfig = ModelConfig.builder().maxTokens(4096).build();

        // 2. 循环
        while (iteration < config.getMaxIterations()) {
            iteration++;
            log.info("[AgentLoop] ===== 第 {} 轮迭代 =====", iteration);

            // 2a. 调用 LLM
            List<ChatMessage> chatMessages = memory.toChatMessages();
            log.info("[AgentLoop] 状态: THINKING -> 调用 LLM (历史消息: {} 条)", chatMessages.size());
            ChatResponse response = llmClient.chat(chatMessages, modelConfig, tools);

            if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                log.warn("[AgentLoop] LLM 返回空响应");
                String errorMsg = "抱歉，我没有收到有效的回复。";
                turns.add(new TurnResult(AgentState.RESPONDING, errorMsg, List.of(), List.of(), iteration));
                return new AgentResponse(errorMsg, turns, iteration, false);
            }

            // 2b. 解析响应
            if (response.hasToolUse()) {
                // 包含 tool_use
                List<ContentBlock> toolUseBlocks = response.getToolUseBlocks();
                List<TurnResult.ToolCall> toolCalls = new ArrayList<>();
                List<ToolResult> toolResults = new ArrayList<>();

                log.info("[AgentLoop] 状态: CALLING_TOOL -> 检测到 {} 个工具调用", toolUseBlocks.size());

                // 构建 assistant 消息（含 tool_use）
                ChatMessage assistantMsg = ChatMessage.assistant(response.getContent());
                memory.addChatMessage(assistantMsg);

                for (ContentBlock block : toolUseBlocks) {
                    String toolUseId = block.getId();
                    String toolName = block.getName();
                    Map<String, Object> arguments = block.getInput() != null ? block.getInput() : Map.of();

                    toolCalls.add(new TurnResult.ToolCall(toolUseId, toolName, arguments));
                    log.info("[AgentLoop] 执行工具: {} (id={}), 参数: {}", toolName, toolUseId, arguments);

                    ToolResult result = toolExecutor.execute(toolName, toolUseId, arguments);
                    toolResults.add(result);
                    log.info("[AgentLoop] 工具结果: {}", result);

                    // 将 tool_result 加入 memory
                    ChatMessage toolResultMsg = ChatMessage.toolResult(toolUseId, result.getContent(), result.isError());
                    memory.addChatMessage(toolResultMsg);
                }

                String assistantText = response.getFirstTextContent();
                if (assistantText == null) {
                    assistantText = "[调用工具]";
                }
                turns.add(new TurnResult(AgentState.CALLING_TOOL, assistantText, toolCalls, toolResults, iteration));
                // 继续循环
            } else {
                // 纯文本响应
                String finalText = response.getFirstTextContent();
                if (finalText == null) {
                    finalText = "(无文本内容)";
                }
                log.info("[AgentLoop] 状态: RESPONDING -> 生成最终回复");

                // 将 assistant 文本消息加入 memory
                ChatMessage assistantMsg = ChatMessage.assistant(finalText);
                memory.addChatMessage(assistantMsg);

                turns.add(new TurnResult(AgentState.RESPONDING, finalText, List.of(), List.of(), iteration));

                log.info("[AgentLoop] 循环结束，总迭代次数: {}", iteration);
                sessionManager.autoSave();
                return new AgentResponse(finalText, turns, iteration, false);
            }
        }

        // 3. 超过 maxIterations
        log.warn("[AgentLoop] 达到最大迭代次数限制: {}", config.getMaxIterations());
        String maxIterMsg = "抱歉，处理您的请求所需的步骤过多，我已达到最大迭代限制。";
        turns.add(new TurnResult(AgentState.RESPONDING, maxIterMsg, List.of(), List.of(), iteration));
        sessionManager.autoSave();
        return new AgentResponse(maxIterMsg, turns, iteration, true);
    }
}
