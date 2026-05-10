package com.agent.tool.fs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class PathValidator {

    @Value("${agent.workspace-root:}")
    private String workspaceRootConfig;

    private Path workspaceRoot;

    @PostConstruct
    public void init() {
        if (workspaceRootConfig == null || workspaceRootConfig.isEmpty()) {
            workspaceRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        } else {
            workspaceRoot = Paths.get(workspaceRootConfig).toAbsolutePath().normalize();
        }
    }

    public Path getWorkspaceRoot() {
        return workspaceRoot;
    }

    public Path validateAndResolve(String path) {
        if (path == null || path.isEmpty()) {
            path = ".";
        }

        Path resolved = workspaceRoot.resolve(path).toAbsolutePath().normalize();

        if (!resolved.startsWith(workspaceRoot)) {
            throw new SecurityException("Access denied: path '" + path + "' is outside the workspace root.");
        }

        return resolved;
    }
}
