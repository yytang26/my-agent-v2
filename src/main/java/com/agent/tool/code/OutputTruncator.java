package com.agent.tool.code;

import org.springframework.stereotype.Component;

@Component
public class OutputTruncator {

    private static final int DEFAULT_MAX_LINES = 200;
    private static final int DEFAULT_MAX_CHARS = 10000;

    public String truncate(String output) {
        return truncate(output, DEFAULT_MAX_LINES, DEFAULT_MAX_CHARS);
    }

    public String truncate(String output, int maxLines, int maxChars) {
        if (output == null || output.isEmpty()) {
            return output;
        }

        String result = output;
        boolean truncated = false;
        int totalLines = output.split("\n", -1).length;

        // 先按字符数截断
        if (result.length() > maxChars) {
            result = result.substring(0, maxChars);
            truncated = true;
        }

        // 再按行数截断
        String[] lines = result.split("\n", -1);
        if (lines.length > maxLines) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < maxLines; i++) {
                if (i > 0) {
                    sb.append("\n");
                }
                sb.append(lines[i]);
            }
            result = sb.toString();
            truncated = true;
        }

        if (truncated) {
            result = result + "\n... (输出已截断，共 " + totalLines + " 行，显示前 " + maxLines + " 行)";
        }

        return result;
    }
}
