package com.agent.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final ApplicationContext applicationContext;
    private final Map<String, ToolMethod> tools = new LinkedHashMap<>();

    public ToolRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        scanAndRegisterTools();
    }

    private void scanAndRegisterTools() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception e) {
                continue;
            }
            Class<?> clazz = bean.getClass();
            for (Method method : clazz.getMethods()) {
                Tool toolAnnotation = method.getAnnotation(Tool.class);
                if (toolAnnotation != null) {
                    String toolName = toolAnnotation.name();
                    ToolDefinition definition = buildToolDefinition(toolAnnotation, method);
                    tools.put(toolName, new ToolMethod(bean, method, definition));
                    log.info("Registered tool: {} -> {}.{}"
                            , toolName, clazz.getSimpleName(), method.getName());
                }
            }
        }
        log.info("Tool registry initialized. Total tools registered: {}", tools.size());
    }

    private ToolDefinition buildToolDefinition(Tool toolAnnotation, Method method) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (Parameter param : method.getParameters()) {
            ToolParam paramAnnotation = param.getAnnotation(ToolParam.class);
            if (paramAnnotation == null) {
                continue;
            }

            Map<String, Object> prop = new LinkedHashMap<>();
            prop.put("type", mapJavaTypeToJsonSchemaType(param.getType()));
            if (!paramAnnotation.description().isEmpty()) {
                prop.put("description", paramAnnotation.description());
            }
            properties.put(paramAnnotation.name(), prop);

            if (paramAnnotation.required()) {
                required.add(paramAnnotation.name());
            }
        }

        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", properties);
        if (!required.isEmpty()) {
            inputSchema.put("required", required);
        }

        return new ToolDefinition(toolAnnotation.name(), toolAnnotation.description(), inputSchema);
    }

    private String mapJavaTypeToJsonSchemaType(Class<?> type) {
        if (type == String.class) {
            return "string";
        } else if (type == int.class || type == Integer.class) {
            return "integer";
        } else if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        } else if (type == double.class || type == Double.class
                || type == float.class || type == Float.class) {
            return "number";
        } else if (type == long.class || type == Long.class) {
            return "integer";
        }
        return "string";
    }

    public List<ToolDefinition> getAllToolDefinitions() {
        return tools.values().stream()
                .map(ToolMethod::definition)
                .toList();
    }

    public Optional<ToolMethod> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public record ToolMethod(Object beanInstance, Method method, ToolDefinition definition) {
    }
}
