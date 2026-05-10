package com.agent.core;

import com.agent.llm.ChunkAccumulator;
import com.agent.llm.LlmClient;
import com.agent.llm.exception.ContextOverflowException;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ContentBlock;
import com.agent.llm.model.ModelConfig;
import com.agent.llm.model.StreamChunk;
import com.agent.context.ContextCompressor;
import com.agent.context.ContextOverflowHandler;
import com.agent.context.PreflightTokenCheck;
import com.agent.memory.ConversationMemory;
import com.agent.memory.Message;
import com.agent.memory.MessageRole;
import com.agent.session.SessionManager;
import com.agent.cli.StreamRenderer;
import com.agent.tool.ToolDefinition;
import com.agent.tool.ToolExecutor;
import com.agent.tool.ToolRegistry;
import com.agent.tool.ToolResult;
import com.agent.tracking.TokenTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class StreamingAgentLoop {

    private static final Logger log = LoggerFactory.getLogger(StreamingAgentLoop.class);

    private final LlmClient llmClient;
    private final ConversationMemory memory;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final StreamRenderer streamRenderer;
    private final TokenTracker tokenTracker;
    private final AgentLoopConfig config;
    private final SessionManager sessionManager;
    private final ContextCompressor contextCompressor;
    private final ContextOverflowHandler contextOverflowHandler;
    private final PreflightTokenCheck preflightTokenCheck;

    @org.springframework.beans.factory.annotation.Value("${llm.context-window-size:200000}")
    private int contextWindowSize = 200000;

    public StreamingAgentLoop(LlmClient llmClient, ConversationMemory memory,
                              ToolRegistry toolRegistry, ToolExecutor toolExecutor,
                              StreamRenderer streamRenderer, TokenTracker tokenTracker,
                              AgentLoopConfig config, SessionManager sessionManager,
                              ContextCompressor contextCompressor,
                              ContextOverflowHandler contextOverflowHandler,
                              PreflightTokenCheck preflightTokenCheck) {
        this.llmClient = llmClient;
        this.memory = memory;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.streamRenderer = streamRenderer;
        this.tokenTracker = tokenTracker;
        this.config = config;
        this.sessionManager = sessionManager;
        this.contextCompressor = contextCompressor;
        this.contextOverflowHandler = contextOverflowHandler;
        this.preflightTokenCheck = preflightTokenCheck;
    }

    public AgentResponse runStreaming(String userMessage) {
        log.info("[StreamingAgentLoop] 开始处理用户消息: {}", userMessage);

        // 1. 将 userMessage 加入 memory
        memory.addMessage(Message.user(userMessage));

        List<TurnResult> turns = new ArrayList<>();
        int iteration = 0;

        List<ToolDefinition> tools = toolRegistry.getAllToolDefinitions();
        ModelConfig modelConfig = ModelConfig.builder().maxTokens(4096).build();

        // 2. 循环
        while (iteration < config.getMaxIterations()) {
            iteration++;
            log.info("[StreamingAgentLoop] ===== 第 {} 轮迭代 =====", iteration);

            List<Message> messages = memory.getMessages();
            List<Message> compressed = contextCompressor.compressIfNeeded(messages);

            // 预飞检查 + 自动压缩
            if (preflightTokenCheck.willOverflow(compressed, contextWindowSize)) {
                log.warn("[StreamingAgentLoop] 预检超限, 启动上下文恢复");
                compressed = contextOverflowHandler.ensureFit(compressed, contextWindowSize);
            }

            List<ChatMessage> chatMessages = toChatMessages(compressed);
            log.info("[StreamingAgentLoop] 状态: THINKING -> 调用 LLM 流式接口 (历史消息: {} 条, 压缩后: {} 条)", messages.size(), chatMessages.size());

            final ChunkAccumulator[] accRef = new ChunkAccumulator[1];
            List<TurnResult.ToolCall> toolCalls = new ArrayList<>();
            List<ToolResult> toolResults = new ArrayList<>();
            int overflowRetry = 0;
            final int maxOverflowRetries = 2;
            boolean streamSuccess = false;

            while (!streamSuccess && overflowRetry <= maxOverflowRetries) {
                final ChunkAccumulator currentAcc = new ChunkAccumulator();
                accRef[0] = currentAcc;
                Flux<StreamChunk> stream = llmClient.chatStream(chatMessages, modelConfig, tools);

                try {
                    stream.doOnNext(chunk -> {
                        if (chunk.isTextDelta()) {
                            streamRenderer.renderChunk(chunk);
                            currentAcc.accumulate(chunk);
                        } else if (chunk.isToolUseStart()) {
                            streamRenderer.renderToolStart(chunk.getToolName());
                            currentAcc.accumulate(chunk);
                        } else if (chunk.isToolInputDelta()) {
                            currentAcc.accumulate(chunk);
                        } else if (chunk.getType() == com.agent.llm.model.StreamEventType.CONTENT_BLOCK_STOP) {
                            currentAcc.accumulate(chunk);
                        } else if (chunk.getType() == com.agent.llm.model.StreamEventType.MESSAGE_START) {
                            currentAcc.accumulate(chunk);
                        } else if (chunk.getType() == com.agent.llm.model.StreamEventType.MESSAGE_DELTA) {
                            currentAcc.accumulate(chunk);
                        } else if (chunk.getType() == com.agent.llm.model.StreamEventType.MESSAGE_STOP) {
                            currentAcc.accumulate(chunk);
                        }
                    }).onErrorResume(e -> {
                        if (e instanceof ContextOverflowException) {
                            return Flux.error(e);
                        }
                        log.error("[StreamingAgentLoop] 流式消费出错", e);
                        return Flux.empty();
                    }).then().block();
                    streamSuccess = true;
                } catch (ContextOverflowException ex) {
                    overflowRetry++;
                    log.warn("[StreamingAgentLoop] 捕获上下文溢出异常 (重试 {}/{}): {}", overflowRetry, maxOverflowRetries, ex.getMessage());
                    if (overflowRetry > maxOverflowRetries) {
                        log.error("[StreamingAgentLoop] 上下文溢出恢复失败, 已达到最大重试次数");
                        throw ex;
                    }
                    List<Message> recovered = contextOverflowHandler.handleOverflow(compressed, ex);
                    chatMessages = toChatMessages(recovered);
                    log.info("[StreamingAgentLoop] 使用压缩后消息重试 ({} 条 -> {} 条)", compressed.size(), recovered.size());
                    compressed = recovered;
                }
            }

            streamRenderer.newLine();

            // c. 流结束后处理
            ChunkAccumulator accumulator = accRef[0];
            ChatResponse response = accumulator != null ? accumulator.toResponse() : null;

            if (accumulator != null && accumulator.hasToolUse()) {
                // 包含 tool_use
                List<ContentBlock> toolUseBlocks = accumulator.getToolUseBlocks();

                log.info("[StreamingAgentLoop] 状态: CALLING_TOOL -> 检测到 {} 个工具调用", toolUseBlocks.size());

                // 构建 assistant 消息（含 tool_use）
                ChatMessage assistantMsg = ChatMessage.assistant(response.getContent());
                memory.addChatMessage(assistantMsg);

                for (ContentBlock block : toolUseBlocks) {
                    String toolUseId = block.getId();
                    String toolName = block.getName();
                    Map<String, Object> arguments = block.getInput() != null ? block.getInput() : Map.of();

                    toolCalls.add(new TurnResult.ToolCall(toolUseId, toolName, arguments));
                    log.info("[StreamingAgentLoop] 执行工具: {} (id={}), 参数: {}", toolName, toolUseId, arguments);

                    ToolResult result = toolExecutor.execute(toolName, toolUseId, arguments);
                    toolResults.add(result);
                    log.info("[StreamingAgentLoop] 工具结果: {}", result);
                    streamRenderer.renderToolResult(result.getContent());

                    // 将 tool_result 加入 memory
                    ChatMessage toolResultMsg = ChatMessage.toolResult(toolUseId, result.getContent(), result.isError());
                    memory.addChatMessage(toolResultMsg);
                }

                String assistantText = accumulator.getCurrentText();
                if (assistantText == null || assistantText.isEmpty()) {
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
                log.info("[StreamingAgentLoop] 状态: RESPONDING -> 生成最终回复");

                // 将 assistant 文本消息加入 memory
                ChatMessage assistantMsg = ChatMessage.assistant(finalText);
                memory.addChatMessage(assistantMsg);

                turns.add(new TurnResult(AgentState.RESPONDING, finalText, List.of(), List.of(), iteration));

                log.info("[StreamingAgentLoop] 循环结束，总迭代次数: {}", iteration);
                sessionManager.autoSave();
                return new AgentResponse(finalText, turns, iteration, false);
            }
        }

        // 3. 超过 maxIterations
        log.warn("[StreamingAgentLoop] 达到最大迭代次数限制: {}", config.getMaxIterations());
        String maxIterMsg = "抱歉，处理您的请求所需的步骤过多，我已达到最大迭代限制。";
        turns.add(new TurnResult(AgentState.RESPONDING, maxIterMsg, List.of(), List.of(), iteration));
        sessionManager.autoSave();
        return new AgentResponse(maxIterMsg, turns, iteration, true);
    }

    private List<ChatMessage> toChatMessages(List<Message> messages) {
        List<ChatMessage> result = new ArrayList<>();
        for (Message msg : messages) {
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
