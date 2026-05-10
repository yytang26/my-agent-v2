package com.agent.tool.code;

import com.agent.tool.Tool;
import com.agent.tool.ToolParam;
import com.agent.tool.fs.PathValidator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
public class GlobTool {

    private final PathValidator pathValidator;
    private final OutputTruncator outputTruncator;

    public GlobTool(PathValidator pathValidator, OutputTruncator outputTruncator) {
        this.pathValidator = pathValidator;
        this.outputTruncator = outputTruncator;
    }

    @Tool(name = "glob", description = "按模式匹配文件路径")
    public String glob(
            @ToolParam(name = "pattern", description = "Glob 模式，如 **/*.java") String pattern,
            @ToolParam(name = "path", description = "搜索根路径（相对于工作区），默认为工作区根", required = false) String path
    ) {
        if (pattern == null || pattern.isEmpty()) {
            return "Error: pattern is required";
        }

        Path searchPath;
        try {
            searchPath = pathValidator.validateAndResolve(path != null && !path.isEmpty() ? path : ".");
        } catch (SecurityException e) {
            return "Error: " + e.getMessage();
        }

        PathMatcher matcher;
        try {
            matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        } catch (Exception e) {
            return "Error: Invalid glob pattern: " + e.getMessage();
        }

        List<String> matches = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(searchPath)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> matcher.matches(searchPath.relativize(p.toAbsolutePath().normalize())))
                .forEach(p -> matches.add(searchPath.relativize(p.toAbsolutePath().normalize()).toString()));
        } catch (IOException e) {
            return "Error: IO error during glob search: " + e.getMessage();
        }

        if (matches.isEmpty()) {
            return "No files matched pattern: " + pattern;
        }

        String result = String.join("\n", matches);
        return outputTruncator.truncate(result);
    }
}
