package com.agent.template;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateRenderer {

    private static final Pattern IF_PATTERN = Pattern.compile(
            "#if\\s+(\\w+)\\s*(.*?)\\s*#/if",
            Pattern.DOTALL
    );

    public String render(String template, Map<String, String> variables) {
        return render(template, variables, null);
    }

    public String render(String template, Map<String, String> values, Map<String, String> defaults) {
        if (template == null) {
            return "";
        }

        String result = template;

        // 1. 处理条件块 #if key ... #/if
        Matcher ifMatcher = IF_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (ifMatcher.find()) {
            String varName = ifMatcher.group(1);
            String innerContent = ifMatcher.group(2);
            String replacement = shouldInclude(varName, values, defaults) ? innerContent : "";
            ifMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        ifMatcher.appendTail(sb);
        result = sb.toString();

        // 2. 替换变量 key
        if (values != null) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() != null ? entry.getValue() : "";
                result = result.replace(key, value);
            }
        }

        // 3. 使用默认值填充未替换的变量
        if (defaults != null) {
            for (Map.Entry<String, String> entry : defaults.entrySet()) {
                String key = entry.getKey();
                if (!result.contains(key)) {
                    continue;
                }
                String defaultValue = entry.getValue() != null ? entry.getValue() : "";
                result = result.replace(key, defaultValue);
            }
        }

        return result;
    }

    private boolean shouldInclude(String varName, Map<String, String> values, Map<String, String> defaults) {
        if (values != null && values.containsKey(varName)) {
            String val = values.get(varName);
            return val != null && !val.isBlank();
        }
        if (defaults != null && defaults.containsKey(varName)) {
            String val = defaults.get(varName);
            return val != null && !val.isBlank();
        }
        return false;
    }
}
