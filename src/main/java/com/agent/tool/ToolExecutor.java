package com.agent.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Optional;

@Component
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final ToolRegistry toolRegistry;

    public ToolExecutor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public ToolResult execute(String toolName, String toolUseId, Map<String, Object> arguments) {
        Optional<ToolRegistry.ToolMethod> toolOpt = toolRegistry.getTool(toolName);
        if (toolOpt.isEmpty()) {
            String errorMsg = "Tool not found: " + toolName;
            log.warn(errorMsg);
            return ToolResult.error(toolUseId, errorMsg);
        }

        ToolRegistry.ToolMethod toolMethod = toolOpt.get();
        Method method = toolMethod.method();
        Object bean = toolMethod.beanInstance();
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            ToolParam paramAnnotation = param.getAnnotation(ToolParam.class);
            if (paramAnnotation == null) {
                continue;
            }
            String paramName = paramAnnotation.name();
            Object value = arguments != null ? arguments.get(paramName) : null;

            if (value == null && paramAnnotation.required()) {
                String errorMsg = "Missing required parameter: " + paramName;
                log.warn(errorMsg);
                return ToolResult.error(toolUseId, errorMsg);
            }

            args[i] = convertValue(value, param.getType());
        }

        try {
            Object result = method.invoke(bean, args);
            String content = result != null ? result.toString() : "";
            log.info("Tool '{}' executed successfully. toolUseId={}", toolName, toolUseId);
            return ToolResult.success(toolUseId, content);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String errorMsg = "Tool execution failed: " + cause.getMessage();
            log.error(errorMsg, cause);
            return ToolResult.error(toolUseId, errorMsg);
        }
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.valueOf(value.toString());
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.valueOf(value.toString());
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.valueOf(value.toString());
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.valueOf(value.toString());
        }
        return value.toString();
    }
}
