package com.agent.react;

import com.agent.core.AgentLoopConfig;
import com.agent.core.AgentResponse;
import com.agent.core.AgentState;
import com.agent.core.TurnResult;
import com.agent.llm.LlmClient;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ModelConfig;
import com.agent.memory.ConversationMemory;
import com.agent.memory.Message;
import com.agent.session.SessionManager;
import com.agent.tool.ToolExecutor;
import com.agent.tool.ToolResult;
import com.agent.tracking.TokenTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ReActAgent {

    private static final Logger log = LoggerFactory.getLogger(ReActAgent.class);

    private final LlmClient llmClient;
    private final ReActPromptBuilder promptBuilder;
    private final ReActParser parser;
    private final LoopDetector loopDetector;
    private final ToolExecutor toolExecutor;
    private final ConversationMemory memory;
    private final TokenTracker tokenTracker;
    private final AgentLoopConfig config;
    private final SessionManager sessionManager;

    public ReActAgent(LlmClient llmClient,
                      ReActPromptBuilder promptBuilder,
                      ReActParser parser,
                      LoopDetector loopDetector,
                      ToolExecutor toolExecutor,
                      ConversationMemory memory,
                      TokenTracker tokenTracker,
                      AgentLoopConfig config,
                      SessionManager sessionManager) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.loopDetector = loopDetector;
        this.toolExecutor = toolExecutor;
        this.memory = memory;
        this.tokenTracker = tokenTracker;
        this.config = config;
        this.sessionManager = sessionManager;
    }

    public AgentResponse run(String userMessage) {
        log.info("[ReActAgent] 开始处理用户消息: {}", userMessage);

        // 1. 构建 ReAct 系统提示词
        String systemPrompt = promptBuilder.buildSystemPrompt();
        memory.setSystemPrompt(systemPrompt);

        // 2. 将用户消息加入 context
        memory.addMessage(Message.user(userMessage));

        List<TurnResult> turns = new ArrayList<>();
        int iteration = 0;
        ModelConfig modelConfig = ModelConfig.builder().maxTokens(4096).build();

        // 3. 循环
        while (iteration < config.getMaxIterations()) {
            iteration++;
            log.info("[ReActAgent] ===== 第 {} 轮迭代 =====", iteration);

            // 3a. 调用 LLM
            List<ChatMessage> chatMessages = buildChatMessages();
            log.info("[ReActAgent] 状态: THINKING -> 调用 LLM (消息数: {})", chatMessages.size());

            ChatResponse response;
            try {
                response = llmClient.chat(chatMessages, modelConfig);
            } catch (Exception e) {
                log.error("[ReActAgent] LLM 调用失败", e);
                String errorMsg = "LLM 调用失败: " + e.getMessage();
                turns.add(new TurnResult(AgentState.RESPONDING, errorMsg, List.of(), List.of(), iteration));
                return new AgentResponse(errorMsg, turns, iteration, false);
            }

            if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                log.warn("[ReActAgent] LLM 返回空响应");
                String errorMsg = "抱歉，我没有收到有效的回复。";
                turns.add(new TurnResult(AgentState.RESPONDING, errorMsg, List.of(), List.of(), iteration));
                return new AgentResponse(errorMsg, turns, iteration, false);
            }

            String llmOutput = response.getFirstTextContent();
            if (llmOutput == null) {
                llmOutput = "";
            }
            log.debug("[ReActAgent] LLM 原始输出:\n{}", llmOutput);

            // 3b. 解析输出
            ReActParser.ReActStep step = parser.parse(llmOutput);

            if (!parser.isValidFormat(llmOutput)) {
                log.warn("[ReActAgent] LLM 输出格式不符合 ReAct 规范");
                // 添加纠正提示，将解析结果加入 context继续
                String correctionPrompt = """
                        请注意：你的输出格式不符合要求。请严格按照以下格式输出：
                        Thought: [你的思考]
                        Action: tool_name(param1="value1")  或  Answer: [你的最终回答]
                        """;
                memory.addMessage(Message.user(correctionPrompt));
                turns.add(new TurnResult(AgentState.THINKING, "格式错误，请求纠正", List.of(), List.of(), iteration));
                continue;
            }

            // 将 assistant 的 ReAct 输出加入 memory
            memory.addMessage(Message.assistant(llmOutput));

            // 如果有 Answer -> 返回最终响应
            if (step.hasAnswer()) {
                String finalAnswer = step.answer();
                log.info("[ReActAgent] 状态: RESPONDING -> 生成最终回答");
                turns.add(new TurnResult(AgentState.RESPONDING, finalAnswer, List.of(), List.of(), iteration));
                log.info("[ReActAgent] 循环结束，总迭代次数: {}", iteration);
                sessionManager.autoSave();
                return new AgentResponse(finalAnswer, turns, iteration, false);
            }

            // 如果有 Action -> 执行工具
            if (step.hasAction()) {
                String action = step.action();
                log.info("[ReActAgent] 状态: CALLING_TOOL -> 解析到 Action: {}", action);

                // 检测循环
                loopDetector.recordAction(action);
                if (loopDetector.isLooping()) {
                    log.warn("[ReActAgent] 检测到循环重复，提示换策略");
                    String loopPrompt = "警告：你最近连续多次执行了相同的动作。请尝试换一种策略或思路来解决问题。";
                    memory.addMessage(Message.user(loopPrompt));
                    turns.add(new TurnResult(AgentState.THINKING, "检测到循环，提示换策略", List.of(), List.of(), iteration));
                    continue;
                }

                // 执行工具
                String toolName = parser.extractToolName(action);
                if (toolName == null) {
                    String errorMsg = "无法从 Action 中解析工具名: " + action;
                    log.warn("[ReActAgent] {}", errorMsg);
                    memory.addMessage(Message.user("Observation: " + errorMsg));
                    turns.add(new TurnResult(AgentState.CALLING_TOOL, errorMsg, List.of(), List.of(), iteration));
                    continue;
                }

                Map<String, String> params = parser.parseActionParams(action);
                log.info("[ReActAgent] 执行工具: {}, 参数: {}", toolName, params);

                Map<String, Object> objectParams = new java.util.HashMap<>(params);
                ToolResult result = toolExecutor.execute(toolName, "react-" + iteration, objectParams);
                log.info("[ReActAgent] 工具结果: {}", result);

                // 将 Observation 追加到 context
                String observation = "Observation: " + (result.isError() ? "错误: " : "") + result.getContent();
                memory.addMessage(Message.user(observation));

                List<TurnResult.ToolCall> toolCalls = List.of(
                        new TurnResult.ToolCall("react-" + iteration, toolName, objectParams));
                List<ToolResult> toolResults = List.of(result);
                turns.add(new TurnResult(AgentState.CALLING_TOOL, step.thought(), toolCalls, toolResults, iteration));

                // 继续循环
                continue;
            }

            // 既没有 Action 也没有 Answer
            log.warn("[ReActAgent] 解析结果既没有 Action 也没有 Answer");
            String fallbackMsg = step.thought() != null ? step.thought() : "无法确定下一步行动。";
            memory.addMessage(Message.user("请继续思考并给出 Action 或 Answer。"));
            turns.add(new TurnResult(AgentState.THINKING, fallbackMsg, List.of(), List.of(), iteration));
        }

        // 4. 超过 maxIterations
        log.warn("[ReActAgent] 达到最大迭代次数限制: {}", config.getMaxIterations());
        String maxIterMsg = "抱歉，处理您的请求所需的步骤过多，我已达到最大迭代限制。";
        turns.add(new TurnResult(AgentState.RESPONDING, maxIterMsg, List.of(), List.of(), iteration));
        sessionManager.autoSave();
        return new AgentResponse(maxIterMsg, turns, iteration, true);
    }

    private List<ChatMessage> buildChatMessages() {
        List<ChatMessage> result = new ArrayList<>();
        String systemPrompt = promptBuilder.buildSystemPrompt();
        result.add(new ChatMessage("system", systemPrompt));

        for (Message msg : memory.getMessages()) {
            // 跳过 system prompt，因为上面已经加了最新的
            if (msg.role() == com.agent.memory.MessageRole.SYSTEM) {
                continue;
            }
            String role = msg.role().name().toLowerCase();
            if (role.equals("tool")) {
                role = "user";
            }
            result.add(new ChatMessage(role, msg.content()));
        }
        return result;
    }
}
