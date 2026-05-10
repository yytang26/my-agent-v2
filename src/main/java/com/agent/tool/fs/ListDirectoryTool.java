package com.agent.tool.fs;

import com.agent.tool.Tool;
import com.agent.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Component
public class ListDirectoryTool {

    private final PathValidator pathValidator;

    public ListDirectoryTool(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    @Tool(name = "list_directory", description = "列出目录内容")
    public String listDirectory(
            @ToolParam(name = "path", description = "目录路径（相对于工作区），默认为根目录", required = false) String path
    ) {
        Path resolvedPath;
        if (path == null || path.isEmpty()) {
            resolvedPath = pathValidator.getWorkspaceRoot();
        } else {
            resolvedPath = pathValidator.validateAndResolve(path);
        }

        if (!Files.exists(resolvedPath)) {
            return "Error: Directory not found: " + path;
        }
        if (!Files.isDirectory(resolvedPath)) {
            return "Error: Path is not a directory: " + path;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Contents of: ").append(resolvedPath).append(System.lineSeparator());
        sb.append("-".repeat(60)).append(System.lineSeparator());

        try (Stream<Path> stream = Files.list(resolvedPath)) {
            stream.sorted().forEach(p -> {
                String name = p.getFileName().toString();
                if (Files.isDirectory(p)) {
                    sb.append(String.format("[DIR]  %-40s%n", name));
                } else {
                    try {
                        long size = Files.size(p);
                        String sizeStr = formatSize(size);
                        sb.append(String.format("[FILE] %-40s %10s%n", name, sizeStr));
                    } catch (IOException e) {
                        sb.append(String.format("[FILE] %-40s %10s%n", name, "?"));
                    }
                }
            });
        } catch (IOException e) {
            return "Error listing directory: " + e.getMessage();
        }

        return sb.toString();
    }

    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024L * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
