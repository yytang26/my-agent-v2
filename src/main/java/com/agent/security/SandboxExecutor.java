package com.agent.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
public class SandboxExecutor {

    private static final Logger log = LoggerFactory.getLogger(SandboxExecutor.class);

    private final boolean sandboxEnabled;
    private final String workspaceRoot;

    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("\\.\\./|\\.\\.\\\\");

    public SandboxExecutor(
            @Value("${agent.security.sandbox-enabled:true}") boolean sandboxEnabled,
            @Value("${agent.workspace-root:${user.dir}}") String workspaceRoot) {
        this.sandboxEnabled = sandboxEnabled;
        this.workspaceRoot = workspaceRoot;
    }

    public SandboxResult execute(String command, SandboxConfig config) {
        if (config == null) {
            config = SandboxConfig.defaults();
        }

        // Pre-execution validation
        SandboxResult validationResult = validateCommand(command, config);
        if (!validationResult.isAllowed()) {
            return validationResult;
        }

        if (!sandboxEnabled) {
            log.warn("Sandbox is disabled, executing command without restrictions: {}", command);
            return runCommand(command, config);
        }

        return runCommand(command, config);
    }

    private SandboxResult validateCommand(String command, SandboxConfig config) {
        if (command == null || command.isBlank()) {
            return SandboxResult.blocked("Empty command");
        }

        // Check blocked commands
        for (String blocked : config.getBlockedCommands()) {
            if (command.contains(blocked)) {
                log.error("Command blocked by blacklist: '{}' contains '{}', command='{}'", blocked, blocked, command);
                return SandboxResult.blocked("Command contains blocked pattern: " + blocked);
            }
        }

        // Check path traversal
        if (PATH_TRAVERSAL_PATTERN.matcher(command).find()) {
            log.error("Command blocked: path traversal detected in '{}'", command);
            return SandboxResult.blocked("Path traversal detected");
        }

        // Validate working directory is within workspace
        String workDir = config.getWorkDir();
        if (workDir != null && !workDir.isBlank()) {
            Path workPath = Paths.get(workDir).toAbsolutePath().normalize();
            Path workspacePath = Paths.get(workspaceRoot).toAbsolutePath().normalize();
            if (!workPath.startsWith(workspacePath)) {
                log.error("Command blocked: work directory '{}' is outside workspace '{}'", workDir, workspaceRoot);
                return SandboxResult.blocked("Work directory is outside allowed workspace");
            }
        }

        return SandboxResult.success(null);
    }

    private SandboxResult runCommand(String command, SandboxConfig config) {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        if (config.getWorkDir() != null && !config.getWorkDir().isBlank()) {
            pb.directory(new File(config.getWorkDir()));
        }
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            boolean finished = process.waitFor(config.getTimeoutMs(), TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                log.warn("Command timed out after {}ms: {}", config.getTimeoutMs(), command);
                return SandboxResult.blocked("Command timed out after " + config.getTimeoutMs() + "ms");
            }

            // Read output with size limit
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                char[] buffer = new char[8192];
                int read;
                long totalBytes = 0;
                while ((read = reader.read(buffer)) != -1) {
                    totalBytes += read;
                    if (totalBytes > config.getMaxOutputBytes()) {
                        log.warn("Command output exceeded max size ({} bytes): {}", config.getMaxOutputBytes(), command);
                        output.append("\n... [output truncated: exceeded max size]");
                        process.destroyForcibly();
                        break;
                    }
                    output.append(buffer, 0, read);
                }
            }

            int exitCode = process.exitValue();
            String result = output.toString();

            if (exitCode != 0) {
                result = "[Exit code: " + exitCode + "]\n" + result;
            }

            log.debug("Sandbox command executed: exitCode={}, command={}", exitCode, command);
            return SandboxResult.success(result);

        } catch (Exception e) {
            log.error("Sandbox command execution failed: {}", command, e);
            return SandboxResult.blocked("Execution failed: " + e.getMessage());
        }
    }
}
