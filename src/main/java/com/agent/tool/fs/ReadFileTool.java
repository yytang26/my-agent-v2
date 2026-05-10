package com.agent.tool.fs;

import com.agent.tool.Tool;
import com.agent.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class ReadFileTool {

    private static final int MAX_LINES_WARNING = 10000;

    private final PathValidator pathValidator;

    public ReadFileTool(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    @Tool(name = "read_file", description = "读取文件内容，支持指定行范围")
    public String readFile(
            @ToolParam(name = "path", description = "文件路径（相对于工作区）") String path,
            @ToolParam(name = "start_line", description = "起始行号（从1开始），不指定则从头开始", required = false) Integer startLine,
            @ToolParam(name = "end_line", description = "结束行号（包含），不指定则到文件末尾", required = false) Integer endLine
    ) {
        Path resolvedPath = pathValidator.validateAndResolve(path);

        if (!Files.exists(resolvedPath)) {
            return "Error: File not found: " + path;
        }
        if (!Files.isRegularFile(resolvedPath)) {
            return "Error: Path is not a file: " + path;
        }

        try {
            List<String> lines = Files.readAllLines(resolvedPath);

            if (lines.size() > MAX_LINES_WARNING && (startLine == null || endLine == null)) {
                return "Warning: File has " + lines.size() + " lines (exceeds " + MAX_LINES_WARNING
                        + "). Please specify start_line and end_line to read a specific range.";
            }

            int start = (startLine != null && startLine > 0) ? startLine - 1 : 0;
            int end = (endLine != null && endLine > 0) ? Math.min(endLine, lines.size()) : lines.size();

            if (start >= lines.size()) {
                return "Error: start_line (" + startLine + ") exceeds file length (" + lines.size() + " lines).";
            }
            if (end < start) {
                return "Error: end_line must be >= start_line.";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                sb.append(lines.get(i));
                if (i < end - 1) {
                    sb.append(System.lineSeparator());
                }
            }
            return sb.toString();
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }
}
