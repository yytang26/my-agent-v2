package com.agent.template;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CommandRegistry {

    private final Map<String, PromptTemplate> commands = new LinkedHashMap<>();

    public CommandRegistry() {
        initBuiltInCommands();
    }

    private void initBuiltInCommands() {
        // /review <file> — 代码审查
        commands.put("review", new PromptTemplate(
                "review",
                "代码审查 - 审查指定文件的代码质量",
                "",
                Map.of("file_path", "")
        ));

        // /explain <file> — 代码解释
        commands.put("explain", new PromptTemplate(
                "explain",
                "代码解释 - 解释指定文件的代码逻辑",
                "",
                Map.of("file_path", "")
        ));

        // /test <file> — 生成测试
        commands.put("test", new PromptTemplate(
                "test",
                "生成测试 - 为指定文件生成单元测试",
                "",
                Map.of("file_path", "")
        ));
    }

    public Optional<PromptTemplate> getCommand(String name) {
        return Optional.ofNullable(commands.get(name));
    }

    public List<String> listCommands() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, PromptTemplate> entry : commands.entrySet()) {
            result.add(String.format("/%s - %s", entry.getKey(), entry.getValue().getDescription()));
        }
        return result;
    }

    public boolean hasCommand(String name) {
        return commands.containsKey(name);
    }
}
