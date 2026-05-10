package com.agent.permission;

import com.agent.cli.InputReader;
import org.jline.terminal.Terminal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InteractivePrompter {

    private final InputReader inputReader;
    private final boolean autoAllow;

    public InteractivePrompter(InputReader inputReader,
                               @Value("${permission.auto-allow:false}") boolean autoAllow) {
        this.inputReader = inputReader;
        this.autoAllow = autoAllow;
    }

    public PermissionResponse prompt(String toolName, Map<String, Object> arguments) {
        if (autoAllow) {
            return PermissionResponse.YES;
        }

        Terminal terminal = inputReader.getTerminal();

        terminal.writer().println();
        terminal.writer().println("⚠️  Agent 想要执行: " + toolName);

        if (arguments != null && !arguments.isEmpty()) {
            StringBuilder argsStr = new StringBuilder("   参数: ");
            boolean first = true;
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                if (!first) {
                    argsStr.append(", ");
                }
                String value = entry.getValue() != null ? entry.getValue().toString() : "null";
                // 截断过长的值
                if (value.length() > 80) {
                    value = value.substring(0, 80) + "...";
                }
                argsStr.append(entry.getKey()).append("=").append(value);
                first = false;
            }
            terminal.writer().println(argsStr);
        }

        while (true) {
            terminal.writer().print("   允许? [y/n/always/deny] > ");
            terminal.writer().flush();

            String line = inputReader.readLineRaw();
            if (line == null) {
                return PermissionResponse.NO;
            }

            String input = line.trim().toLowerCase();
            switch (input) {
                case "y", "yes" -> {
                    return PermissionResponse.YES;
                }
                case "n", "no" -> {
                    return PermissionResponse.NO;
                }
                case "always", "a" -> {
                    return PermissionResponse.ALWAYS;
                }
                case "deny", "d", "deny_forever", "deny-forever" -> {
                    return PermissionResponse.DENY_FOREVER;
                }
                default -> terminal.writer().println("   请输入 y, n, always 或 deny");
            }
        }
    }
}
