package com.agent.tool;

import com.agent.permission.PermissionManager;
import com.agent.security.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Component
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final ToolRegistry toolRegistry;
    private final PermissionManager permissionManager;
    private final SecurityGateway securityGateway;

    public ToolExecutor(ToolRegistry toolRegistry, PermissionManager permissionManager, SecurityGateway securityGateway) {
        this.toolRegistry = toolRegistry;
        this.permissionManager = permissionManager;
        this.securityGateway = securityGateway;
    }

    public ToolResult execute(String toolName, String toolUseId, Map<String, Object> arguments) {
        return execute(toolName, toolUseId, arguments, null);
    }

    public ToolResult execute(String toolName, String toolUseId, Map<String, Object> arguments, String userInput) {
        // 1. Security check
        SecurityCheckResult securityCheck = securityGateway.check(toolName, arguments, userInput);

        if (!securityCheck.isAllowed()) {
            log.warn("Security blocked tool '{}': {}", toolName, securityCheck.getMessage());
            return ToolResult.error(toolUseId, "Security check failed: " + securityCheck.getMessage());
        }

        // 2. Human escalation approval
        if (securityCheck.isNeedsHumanApproval()) {
            EscalationDecision escalation = securityCheck.getEscalationDecision();
            boolean approved = securityGateway.getHumanEscalation().requestHumanApproval(
                escalation != null ? escalation.getMessage() : "High risk operation requires approval: " + toolName
            );
            if (!approved) {
                log.warn("Human denied approval for tool '{}': {}", toolName, securityCheck.getMessage());
                return ToolResult.error(toolUseId, "Human approval denied: " + securityCheck.getMessage());
            }
        }

        // 3. Permission check
        if (!permissionManager.checkPermission(toolName, arguments)) {
            log.warn("用户拒绝了工具执行: {}", toolName);
            return ToolResult.error(toolUseId, "用户拒绝了该操作");
        }

        // 4. Sandbox execution for bash
        if (securityCheck.isUseSandbox() && "bash".equals(toolName)) {
            return executeBashInSandbox(toolName, toolUseId, arguments);
        }

        // 5. Normal execution
        ToolResult result = doExecute(toolName, toolUseId, arguments);

        // 6. Sanitize output
        if (result != null && !result.isError() && result.getContent() != null) {
            String sanitized = securityGateway.sanitizeOutput(result.getContent());
            if (!sanitized.equals(result.getContent())) {
                result = ToolResult.success(toolUseId, sanitized);
            }
        }

        return result;
    }

    private ToolResult executeBashInSandbox(String toolName, String toolUseId, Map<String, Object> arguments) {
        String command = "";
        if (arguments != null && arguments.containsKey("command")) {
            Object cmdObj = arguments.get("command");
            command = cmdObj != null ? cmdObj.toString() : "";
        }

        log.info("Executing bash command in sandbox: {}", command);
        SandboxResult sandboxResult = securityGateway.getSandboxExecutor().execute(command, null);

        if (!sandboxResult.isAllowed()) {
            log.warn("Sandbox blocked bash command: {}", sandboxResult.getBlockedReason());
            return ToolResult.error(toolUseId, "Sandbox blocked: " + sandboxResult.getBlockedReason());
        }

        String output = sandboxResult.getOutput();
        // Sanitize output
        output = securityGateway.sanitizeOutput(output);

        return ToolResult.success(toolUseId, output);
    }

    private ToolResult doExecute(String toolName, String toolUseId, Map<String, Object> arguments) {
        Optional<ToolRegistry.ToolMethod> toolOpt = toolRegistry.getTool(toolName);
        if (toolOpt.isPresent()) {
            return executeAnnotatedTool(toolOpt.get(), toolName, toolUseId, arguments);
        }

        Optional<Function<Map<String, Object>, ToolResult>> dynamicOpt = toolRegistry.getDynamicExecutor(toolName);
        if (dynamicOpt.isPresent()) {
            try {
                ToolResult result = dynamicOpt.get().apply(arguments);
                log.info("Dynamic tool '{}' executed successfully. toolUseId={}", toolName, toolUseId);
                return result;
            } catch (Exception e) {
                String errorMsg = "Dynamic tool execution failed: " + e.getMessage();
                log.error(errorMsg, e);
                return ToolResult.error(toolUseId, errorMsg);
            }
        }

        String errorMsg = "Tool not found: " + toolName;
        log.warn(errorMsg);
        return ToolResult.error(toolUseId, errorMsg);
    }

    private ToolResult executeAnnotatedTool(ToolRegistry.ToolMethod toolMethod, String toolName, String toolUseId, Map<String, Object> arguments) {
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
