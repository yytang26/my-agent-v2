package com.agent.routing;

import com.agent.tool.ToolDefinition;
import com.agent.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IntentRouter {

    private static final Logger log = LoggerFactory.getLogger(IntentRouter.class);

    private final IntentClassifier intentClassifier;
    private final RoutingConfig routingConfig;
    private final ToolRegistry toolRegistry;

    public IntentRouter(IntentClassifier intentClassifier,
                        RoutingConfig routingConfig,
                        ToolRegistry toolRegistry) {
        this.intentClassifier = intentClassifier;
        this.routingConfig = routingConfig;
        this.toolRegistry = toolRegistry;
    }

    public List<ToolDefinition> route(String userMessage) {
        if (!routingConfig.isEnabled()) {
            List<ToolDefinition> all = toolRegistry.getAllToolDefinitions();
            log.debug("[IntentRouter] 路由未启用，返回所有 {} 个工具", all.size());
            return all;
        }

        Intent intent = intentClassifier.classify(userMessage);
        List<String> toolNames = routingConfig.getToolsForIntent(intent);

        List<ToolDefinition> result = new ArrayList<>();
        for (String name : toolNames) {
            toolRegistry.getTool(name)
                    .map(ToolRegistry.ToolMethod::definition)
                    .ifPresent(result::add);
        }

        log.info("[IntentRouter] 意图识别: [{}] -> 激活工具: {}", intent, toolNames);
        return result;
    }
}
