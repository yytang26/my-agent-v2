package com.agent.web;

import com.agent.context.ContextCompressor;
import com.agent.context.ContextOverflowHandler;
import com.agent.context.PreflightTokenCheck;
import com.agent.core.AgentLoopConfig;
import com.agent.core.AgentResponse;
import com.agent.core.AgentState;
import com.agent.core.TurnResult;
import com.agent.llm.ChunkAccumulator;
import com.agent.llm.LlmClient;
import com.agent.llm.exception.ContextOverflowException;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ContentBlock;
import com.agent.llm.model.ModelConfig;
import com.agent.llm.model.StreamChunk;
import com.agent.memory.ConversationMemory;
import com.agent.memory.Message;
import com.agent.memory.MessageRole;
import com.agent.routing.IntentRouter;
import com.agent.tool.ToolDefinition;
import com.agent.tool.ToolExecutor;
import com.agent.tool.ToolRegistry;
import com.agent.tool.ToolResult;
import com.agent.tracking.TokenTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class WebAgentLoop {

    private static final Logger log = LoggerFactory.getLogger(WebAgentLoop.class);

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final TokenTracker tokenTracker;
    private final AgentLoopConfig config;
    private final ContextCompressor contextCompressor;
    private final ContextOverflowHandler contextOverflowHandler;
    private final PreflightTokenCheck preflightTokenCheck;
    private final IntentRouter intentRouter;

    @Value("${llm.context-window-size:200000}")
    private int contextWindowSize = 200000;

    public WebAgentLoop(LlmClient llmClient, ToolRegistry toolRegistry,
                        ToolExecutor toolExecutor, TokenTracker tokenTracker,
                        AgentLoopConfig config, ContextCompressor contextCompressor,
                        ContextOverflowHandler contextOverflowHandler,
                        PreflightTokenCheck preflightTokenCheck,
                        IntentRouter intentRouter) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.tokenTracker = tokenTracker;
        this.config = config;
        this.contextCompressor = contextCompressor;
        this.contextOverflowHandler = contextOverflowHandler;
        this.preflightTokenCheck = preflightTokenCheck;
        this.intentRouter = intentRouter;
    }

    public AgentResponse run(String userMessage, ConversationMemory memory, SseEmitter emitter) {
        log.info("[WebAgentLoop] 开始处理用户消息: {}", userMessage);

        memory.addMessage(Message.user(userMessage));

        List<TurnResult> turns = new ArrayList<>();
        int iteration = 0;

        List<ToolDefinition> tools = intentRouter.route(userMessage);
        ModelConfig modelConfig = ModelConfig.builder().maxTokens(4096).build();

        try {
            while (iteration < config.getMaxIterations()) {
                iteration++;
                log.info("[WebAgentLoop] ===== 第 {} 轮迭代 =====", iteration);

                List<Message> messages = memory.getMessages();
                List<Message> compressed = contextCompressor.compressIfNeeded(messages);

                if (preflightTokenCheck.willOverflow(compressed, contextWindowSize)) {
                    log.warn("[WebAgentLoop] 预检超限, 启动上下文恢复");
                    compressed = contextOverflowHandler.ensureFit(compressed, contextWindowSize);
                }

                List<ChatMessage> chatMessages = toChatMessages(compressed);
                log.info("[WebAgentLoop] 调用 LLM 流式接口 (历史消息: {} 条)", messages.size());

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
                                currentAcc.accumulate(chunk);
                                sendEvent(emitter, "text", Map.of("delta", chunk.getText()));
                            } else if (chunk.isToolUseStart()) {
                                currentAcc.accumulate(chunk);
                                sendEvent(emitter, "tool_start", Map.of(
                                        "name", chunk.getToolName(),
                                        "id", chunk.getToolUseId()
                                ));
                            } else if (chunk.isToolInputDelta()) {
                                currentAcc.accumulate(chunk);
                            } else if (chunk.getType() == com.agent.llm.model.StreamEventType.CONTENT_BLOCK_STOP) {
                                currentAcc.accumulate(chunk);
                            } else if (chunk.getType() == com.agent.llm.model.StreamEventType.MESSAGE_START
                                    || chunk.getType() == com.agent.llm.model.StreamEventType.MESSAGE_DELTA
                                    || chunk.getType() == com.agent.llm.model.StreamEventType.MESSAGE_STOP) {
                                currentAcc.accumulate(chunk);
                            }
                        }).onErrorResume(e -> {
                            if (e instanceof ContextOverflowException) {
                                return Flux.error(e);
                            }
                            log.error("[WebAgentLoop] 流式消费出错", e);
                            return Flux.empty();
                        }).then().block();
                        streamSuccess = true;
                    } catch (ContextOverflowException ex) {
                        overflowRetry++;
                        log.warn("[WebAgentLoop] 捕获上下文溢出异常 (重试 {}/{}): {}", overflowRetry, maxOverflowRetries, ex.getMessage());
                        if (overflowRetry > maxOverflowRetries) {
                            log.error("[WebAgentLoop] 上下文溢出恢复失败, 已达到最大重试次数");
                            throw ex;
                        }
                        List<Message> recovered = contextOverflowHandler.handleOverflow(compressed, ex);
                        chatMessages = toChatMessages(recovered);
                        log.info("[WebAgentLoop] 使用压缩后消息重试 ({} 条 -> {} 条)", compressed.size(), recovered.size());
                        compressed = recovered;
                    }
                }

                ChunkAccumulator accumulator = accRef[0];
                ChatResponse response = accumulator != null ? accumulator.toResponse() : null;

                if (accumulator != null && accumulator.hasToolUse()) {
                    List<ContentBlock> toolUseBlocks = accumulator.getToolUseBlocks();
                    log.info("[WebAgentLoop] 状态: CALLING_TOOL -> 检测到 {} 个工具调用", toolUseBlocks.size());

                    ChatMessage assistantMsg = ChatMessage.assistant(response.getContent());
                    memory.addChatMessage(assistantMsg);

                    for (ContentBlock block : toolUseBlocks) {
                        String toolUseId = block.getId();
                        String toolName = block.getName();
                        Map<String, Object> arguments = block.getInput() != null ? block.getInput() : Map.of();

                        toolCalls.add(new TurnResult.ToolCall(toolUseId, toolName, arguments));
                        log.info("[WebAgentLoop] 执行工具: {} (id={}), 参数: {}", toolName, toolUseId, arguments);

                        ToolResult result = toolExecutor.execute(toolName, toolUseId, arguments);
                        toolResults.add(result);
                        log.info("[WebAgentLoop] 工具结果: {}", result);

                        sendEvent(emitter, "tool_result", Map.of(
                                "name", toolName,
                                "id", toolUseId,
                                "result", result.getContent(),
                                "error", result.isError()
                        ));

                        ChatMessage toolResultMsg = ChatMessage.toolResult(toolUseId, result.getContent(), result.isError());
                        memory.addChatMessage(toolResultMsg);
                    }

                    String assistantText = accumulator.getCurrentText();
                    if (assistantText == null || assistantText.isEmpty()) {
                        assistantText = "[调用工具]";
                    }
                    turns.add(new TurnResult(AgentState.CALLING_TOOL, assistantText, toolCalls, toolResults, iteration));
                } else {
                    String finalText = response != null ? response.getFirstTextContent() : null;
                    if (finalText == null) {
                        finalText = "(无文本内容)";
                    }
                    log.info("[WebAgentLoop] 状态: RESPONDING -> 生成最终回复");

                    ChatMessage assistantMsg = ChatMessage.assistant(finalText);
                    memory.addChatMessage(assistantMsg);

                    turns.add(new TurnResult(AgentState.RESPONDING, finalText, List.of(), List.of(), iteration));

                    log.info("[WebAgentLoop] 循环结束，总迭代次数: {}", iteration);
                    sendEvent(emitter, "done", Map.of("iterations", iteration));
                    return new AgentResponse(finalText, turns, iteration, false);
                }
            }

            log.warn("[WebAgentLoop] 达到最大迭代次数限制: {}", config.getMaxIterations());
            String maxIterMsg = "抱歉，处理您的请求所需的步骤过多，我已达到最大迭代限制。";
            turns.add(new TurnResult(AgentState.RESPONDING, maxIterMsg, List.of(), List.of(), iteration));
            sendEvent(emitter, "done", Map.of("iterations", iteration, "maxReached", true));
            return new AgentResponse(maxIterMsg, turns, iteration, true);

        } catch (Exception e) {
            log.error("[WebAgentLoop] 执行出错", e);
            sendEvent(emitter, "error", Map.of("message", e.getMessage()));
            throw e;
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception e) {
            log.warn("[WebAgentLoop] 发送 SSE 事件失败: {}", e.getMessage());
        }
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
