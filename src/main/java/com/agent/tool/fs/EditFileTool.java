package com.agent.tool.fs;

import com.agent.tool.Tool;
import com.agent.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class EditFileTool {

    private final PathValidator pathValidator;

    public EditFileTool(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    @Tool(name = "edit_file", description = "编辑文件，将指定的旧文本替换为新文本")
    public String editFile(
            @ToolParam(name = "path", description = "文件路径（相对于工作区）") String path,
            @ToolParam(name = "old_text", description = "要被替换的原文本") String oldText,
            @ToolParam(name = "new_text", description = "替换后的新文本") String newText
    ) {
        Path resolvedPath = pathValidator.validateAndResolve(path);

        if (!Files.exists(resolvedPath)) {
            return "Error: File not found: " + path;
        }
        if (!Files.isRegularFile(resolvedPath)) {
            return "Error: Path is not a file: " + path;
        }

        try {
            String content = Files.readString(resolvedPath);

            int index = content.indexOf(oldText);
            if (index == -1) {
                return "Error: old_text not found in file: " + path;
            }

            String newContent = content.substring(0, index) + newText + content.substring(index + oldText.length());
            Files.writeString(resolvedPath, newContent);
            return "File edited successfully: " + path;
        } catch (IOException e) {
            return "Error editing file: " + e.getMessage();
        }
    }
}
