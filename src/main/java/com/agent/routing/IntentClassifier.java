package com.agent.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class IntentClassifier {

    private static final Logger log = LoggerFactory.getLogger(IntentClassifier.class);

    private static final Map<Intent, List<String>> KEYWORDS = Map.of(
            Intent.CODE, List.of(
                    "代码", "函数", "类", "方法", "bug", "修复", "重构", "实现", "todo", "编译",
                    "code", "function", "class", "method", "fix", "refactor", "implement", "compile"
            ),
            Intent.FILE, List.of(
                    "文件", "创建", "写入", "读取", "编辑", "目录",
                    "file", "create", "write", "read", "edit", "directory", "folder"
            ),
            Intent.SEARCH, List.of(
                    "查找", "搜索", "找", "哪些文件",
                    "search", "find", "grep", "glob", "locate"
            ),
            Intent.SYSTEM, List.of(
                    "运行", "执行", "命令", "版本", "安装",
                    "run", "execute", "command", "version", "install", "build", "mvn", "gradle"
            )
    );

    private final RoutingConfig routingConfig;

    public IntentClassifier(RoutingConfig routingConfig) {
        this.routingConfig = routingConfig;
    }

    public Intent classify(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return Intent.GENERAL;
        }

        if (routingConfig.isUseLlm()) {
            return classifyByLlm(userMessage);
        }

        return classifyByRules(userMessage);
    }

    private Intent classifyByRules(String userMessage) {
        String lower = userMessage.toLowerCase(Locale.ROOT);

        for (Map.Entry<Intent, List<String>> entry : KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                    log.debug("[IntentClassifier] 规则匹配: '{}' -> {}", userMessage, entry.getKey());
                    return entry.getKey();
                }
            }
        }

        return Intent.GENERAL;
    }

    private Intent classifyByLlm(String userMessage) {
        log.debug("[IntentClassifier] LLM 分类未实现，回退到规则分类: {}", userMessage);
        return classifyByRules(userMessage);
    }
}
