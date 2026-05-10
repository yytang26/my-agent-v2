package com.agent.react;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReActParser {

    private static final Pattern THOUGHT_PATTERN = Pattern.compile(
            "Thought:\\s*(.*?)(?=\\n(?:Action:|Answer:|$))",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern ACTION_PATTERN = Pattern.compile(
            "Action:\\s*([^(\\n]+?)\\s*\\((.*?)\\)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern ANSWER_PATTERN = Pattern.compile(
            "Answer:\\s*(.*?)(?=\\n(?:Thought:|Action:|$)|$)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern PARAM_PATTERN = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*\"(.*?)\"");

    public ReActStep parse(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) {
            return fallbackStep("LLM 输出为空");
        }

        String thought = extractThought(llmOutput);
        String answer = extractAnswer(llmOutput);

        if (answer != null && !answer.isBlank()) {
            return new ReActStep(thought != null ? thought : "", null, answer.trim());
        }

        ParsedAction parsedAction = extractAction(llmOutput);
        if (parsedAction != null) {
            String actionStr = parsedAction.toolName + "(" + formatParams(parsedAction.params) + ")";
            return new ReActStep(
                    thought != null ? thought : "",
                    actionStr,
                    null);
        }

        // 格式违反：尝试从纯文本中提取意图
        return fallbackStep(llmOutput);
    }

    public boolean isValidFormat(String output) {
        if (output == null || output.isBlank()) {
            return false;
        }
        boolean hasThought = THOUGHT_PATTERN.matcher(output).find();
        boolean hasAction = ACTION_PATTERN.matcher(output).find();
        boolean hasAnswer = ANSWER_PATTERN.matcher(output).find();
        return hasThought && (hasAction || hasAnswer);
    }

    private String extractThought(String output) {
        Matcher matcher = THOUGHT_PATTERN.matcher(output);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extractAnswer(String output) {
        Matcher matcher = ANSWER_PATTERN.matcher(output);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private ParsedAction extractAction(String output) {
        Matcher matcher = ACTION_PATTERN.matcher(output);
        if (matcher.find()) {
            String toolName = matcher.group(1).trim();
            String paramsStr = matcher.group(2).trim();
            Map<String, String> params = parseParams(paramsStr);
            return new ParsedAction(toolName, params);
        }
        return null;
    }

    private Map<String, String> parseParams(String paramsStr) {
        Map<String, String> params = new LinkedHashMap<>();
        Matcher matcher = PARAM_PATTERN.matcher(paramsStr);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            params.put(key, value);
        }
        return params;
    }

    private String formatParams(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
            first = false;
        }
        return sb.toString();
    }

    private ReActStep fallbackStep(String rawOutput) {
        String thought = "无法解析 LLM 的标准 ReAct 格式输出，尝试从文本中提取意图。";
        return new ReActStep(thought, null, rawOutput.trim());
    }

    public Map<String, String> parseActionParams(String action) {
        ParsedAction parsed = extractAction(action);
        if (parsed != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : parsed.params.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
            @SuppressWarnings("unchecked")
            Map<String, String> stringMap = (Map<String, String>) (Map<?, ?>) result;
            return stringMap;
        }
        return Map.of();
    }

    public String extractToolName(String action) {
        ParsedAction parsed = extractAction(action);
        return parsed != null ? parsed.toolName : null;
    }

    private record ParsedAction(String toolName, Map<String, String> params) {
    }

    public record ReActStep(String thought, String action, String answer) {
        public boolean hasAction() {
            return action != null && !action.isBlank();
        }

        public boolean hasAnswer() {
            return answer != null && !answer.isBlank();
        }
    }
}
