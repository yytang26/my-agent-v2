package com.agent.tool.fs;

import com.agent.tool.Tool;
import com.agent.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class WriteFileTool {

    private final PathValidator pathValidator;

    public WriteFileTool(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    @Tool(name = "write_file", description = "写入或创建文件")
    public String writeFile(
            @ToolParam(name = "path", description = "文件路径（相对于工作区）") String path,
            @ToolParam(name = "content", description = "文件内容") String content
    ) {
        Path resolvedPath = pathValidator.validateAndResolve(path);

        try {
            Path parent = resolvedPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.writeString(resolvedPath, content);
            return "File written successfully: " + path;
        } catch (IOException e) {
            return "Error writing file: " + e.getMessage();
        }
    }
}
