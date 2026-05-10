package com.agent.tool.code;

import com.agent.tool.Tool;
import com.agent.tool.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class BashTool {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final ProcessRunner processRunner;
    private final OutputTruncator outputTruncator;

    public BashTool(ProcessRunner processRunner, OutputTruncator outputTruncator) {
        this.processRunner = processRunner;
        this.outputTruncator = outputTruncator;
    }

    @Tool(name = "bash", description = "执行 Shell 命令")
    public String bash(
            @ToolParam(name = "command", description = "要执行的命令") String command,
            @ToolParam(name = "timeout", description = "超时时间（秒），默认30", required = false) Integer timeout
    ) {
        if (command == null || command.isEmpty()) {
            return "Error: command is required";
        }

        int timeoutSeconds = timeout != null && timeout > 0 ? timeout : DEFAULT_TIMEOUT_SECONDS;
        long timeoutMs = timeoutSeconds * 1000L;

        ProcessRunner.ProcessResult result = processRunner.run(command, timeoutMs);

        if (result.timedOut()) {
            String partialOutput = outputTruncator.truncate(result.output());
            return "命令执行超时（" + timeoutSeconds + "秒）\n部分输出:\n" + partialOutput;
        }

        String output = outputTruncator.truncate(result.output());
        return "Exit code: " + result.exitCode() + "\n---\n" + output;
    }
}
