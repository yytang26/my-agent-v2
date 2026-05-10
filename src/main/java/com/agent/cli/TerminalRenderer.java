package com.agent.cli;

import org.springframework.stereotype.Component;

@Component
public class TerminalRenderer {

    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_BOLD = "\033[1m";
    private static final String ANSI_BRIGHT = "\033[1;97m";
    private static final String ANSI_CYAN = "\033[36m";
    private static final String ANSI_GRAY_BG = "\033[48;5;240m";
    private static final String ANSI_GREEN = "\033[32m";
    private static final String ANSI_YELLOW = "\033[33m";

    public String render(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        String[] lines = markdown.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inCodeBlock = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // 代码块边界
            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                if (!inCodeBlock) {
                    result.append(ANSI_RESET).append("\n");
                } else {
                    result.append(ANSI_GRAY_BG);
                }
                continue;
            }

            if (inCodeBlock) {
                result.append("    ").append(ANSI_CYAN).append(line).append(ANSI_RESET).append("\n");
                continue;
            }

            // 标题
            if (trimmed.startsWith("# ")) {
                result.append(ANSI_BOLD).append(ANSI_BRIGHT)
                      .append(trimmed.substring(2))
                      .append(ANSI_RESET).append("\n");
                continue;
            }
            if (trimmed.startsWith("## ")) {
                result.append(ANSI_BOLD).append(ANSI_BRIGHT)
                      .append(trimmed.substring(3))
                      .append(ANSI_RESET).append("\n");
                continue;
            }
            if (trimmed.startsWith("### ")) {
                result.append(ANSI_BOLD)
                      .append(trimmed.substring(4))
                      .append(ANSI_RESET).append("\n");
                continue;
            }

            // 列表
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                String content = trimmed.substring(2);
                content = renderInline(content);
                result.append(ANSI_GREEN).append("  • ").append(ANSI_RESET)
                      .append(content).append("\n");
                continue;
            }

            // 数字列表
            if (trimmed.matches("^\\d+\\.\\s.*")) {
                int spaceIdx = trimmed.indexOf(' ');
                String num = trimmed.substring(0, spaceIdx);
                String content = trimmed.substring(spaceIdx + 1);
                content = renderInline(content);
                result.append(ANSI_GREEN).append("  ").append(num).append(" ").append(ANSI_RESET)
                      .append(content).append("\n");
                continue;
            }

            // 普通行，处理内联样式
            result.append(renderInline(line)).append("\n");
        }

        return result.toString().trim();
    }

    private String renderInline(String text) {
        // 粗体 **text**
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", ANSI_BOLD + "$1" + ANSI_RESET);
        // 斜体 *text*
        text = text.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", ANSI_YELLOW + "$1" + ANSI_RESET);
        // 行内代码 `text`
        text = text.replaceAll("`(.+?)`", ANSI_CYAN + "$1" + ANSI_RESET);
        return text;
    }
}
