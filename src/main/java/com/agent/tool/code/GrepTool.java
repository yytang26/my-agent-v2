package com.agent.tool.code;

import com.agent.tool.Tool;
import com.agent.tool.ToolParam;
import com.agent.tool.fs.PathValidator;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

@Component
public class GrepTool {

    private final PathValidator pathValidator;
    private final OutputTruncator outputTruncator;

    public GrepTool(PathValidator pathValidator, OutputTruncator outputTruncator) {
        this.pathValidator = pathValidator;
        this.outputTruncator = outputTruncator;
    }

    @Tool(name = "grep", description = "在文件中搜索匹配正则表达式的内容")
    public String grep(
            @ToolParam(name = "pattern", description = "正则表达式模式") String pattern,
            @ToolParam(name = "path", description = "搜索路径（文件或目录，相对于工作区）", required = false) String path,
            @ToolParam(name = "include", description = "文件名过滤（如 *.java）", required = false) String include
    ) {
        if (pattern == null || pattern.isEmpty()) {
            return "Error: pattern is required";
        }

        Pattern regex;
        try {
            regex = Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            return "Error: Invalid regex pattern: " + e.getMessage();
        }

        Path searchPath;
        try {
            searchPath = pathValidator.validateAndResolve(path != null && !path.isEmpty() ? path : ".");
        } catch (SecurityException e) {
            return "Error: " + e.getMessage();
        }

        final PathMatcher includeMatcher;
        if (include != null && !include.isEmpty()) {
            includeMatcher = FileSystems.getDefault().getPathMatcher("glob:" + include);
        } else {
            includeMatcher = null;
        }

        List<String> matches = new ArrayList<>();

        try {
            if (Files.isRegularFile(searchPath)) {
                grepFile(searchPath, regex, searchPath.getParent(), matches);
            } else if (Files.isDirectory(searchPath)) {
                try (Stream<Path> walk = Files.walk(searchPath)) {
                    walk.filter(Files::isRegularFile)
                        .filter(p -> includeMatcher == null || includeMatcher.matches(p.getFileName()))
                        .forEach(p -> grepFile(p, regex, searchPath, matches));
                }
            } else {
                return "Error: path does not exist: " + path;
            }
        } catch (IOException e) {
            return "Error: IO error during search: " + e.getMessage();
        }

        if (matches.isEmpty()) {
            return "No matches found for pattern: " + pattern;
        }

        String result = String.join("\n", matches);
        return outputTruncator.truncate(result);
    }

    private void grepFile(Path file, Pattern regex, Path basePath, List<String> matches) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            int lineNum = 0;
            Path relativePath = basePath.relativize(file.toAbsolutePath().normalize());
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (regex.matcher(line).find()) {
                    matches.add(relativePath + ":" + lineNum + ":" + line);
                }
            }
        } catch (IOException e) {
            // Skip files that can't be read
        }
    }
}
