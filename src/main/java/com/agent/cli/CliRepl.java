package com.agent.cli;

import com.agent.core.AgentLoop;
import com.agent.core.AgentResponse;
import com.agent.core.StreamingAgentLoop;
import com.agent.memory.ConversationMemory;
import com.agent.tracking.TokenTracker;
import com.agent.tracking.UsageSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CliRepl {

    private static final Logger log = LoggerFactory.getLogger(CliRepl.class);

    private final InputReader inputReader;
    private final CommandHandler commandHandler;
    private final AgentLoop agentLoop;
    private final TerminalRenderer terminalRenderer;
    private final Spinner spinner;
    private final ConversationMemory conversationMemory;
    private final TokenTracker tokenTracker;
    private final StreamingAgentLoop streamingAgentLoop;

    public CliRepl(InputReader inputReader,
                   CommandHandler commandHandler,
                   AgentLoop agentLoop,
                   TerminalRenderer terminalRenderer,
                   Spinner spinner,
                   ConversationMemory conversationMemory,
                   TokenTracker tokenTracker,
                   @Autowired(required = false) StreamingAgentLoop streamingAgentLoop) {
        this.inputReader = inputReader;
        this.commandHandler = commandHandler;
        this.agentLoop = agentLoop;
        this.terminalRenderer = terminalRenderer;
        this.spinner = spinner;
        this.conversationMemory = conversationMemory;
        this.tokenTracker = tokenTracker;
        this.streamingAgentLoop = streamingAgentLoop;
    }

    public void start() {
        printWelcome();

        conversationMemory.setSystemPrompt("你是一个有帮助的 AI 编程助手，可以使用工具来帮助用户完成任务。");

        while (true) {
            String input = inputReader.readLine();

            if (input == null || inputReader.isEof()) {
                System.out.println("\n再见！");
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            if (commandHandler.isCommand(input)) {
                String result = commandHandler.handleCommand(input);
                if (CommandHandler.EXIT_SIGNAL.equals(result)) {
                    System.out.println("\n再见！");
                    break;
                }
                System.out.println(result);
                continue;
            }

            // 调用 AgentLoop（优先流式模式）
            AgentResponse response;
            try {
                if (streamingAgentLoop != null) {
                    response = streamingAgentLoop.runStreaming(input);
                } else {
                    spinner.start();
                    response = agentLoop.run(input);
                    spinner.stop();
                }
            } catch (Exception e) {
                log.error("AgentLoop 执行出错", e);
                spinner.stop();
                System.out.println("错误: " + e.getMessage());
                continue;
            }

            // 流式模式下文字已实时输出，非流式模式需要渲染最终结果
            if (streamingAgentLoop == null) {
                String finalMessage = response.getFinalMessage();
                if (finalMessage != null && !finalMessage.isEmpty()) {
                    String rendered = terminalRenderer.render(finalMessage);
                    System.out.println(rendered);
                }
            }
        }

        printFarewell();
    }

    private void printWelcome() {
        System.out.println("\n\u256D\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u256E");
        System.out.println("\u2502  My Agent v0.1 - AI 编程助手        \u2502");
        System.out.println("\u2502  输入消息开始对话，/help 查看帮助    \u2502");
        System.out.println("\u2570\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u256F\n");
    }

    private void printFarewell() {
        UsageSummary summary = tokenTracker.getSummary();
        System.out.println("\n\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        System.out.println("会话结束 - 费用摘要:");
        System.out.println(summary);
        System.out.println("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n");
    }
}
