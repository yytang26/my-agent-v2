package com.agent.security;

import java.util.Set;

public class SandboxConfig {

    public static final long DEFAULT_TIMEOUT_MS = 30000L;
    public static final long DEFAULT_MAX_OUTPUT_BYTES = 1024 * 1024; // 1MB

    private long timeoutMs = DEFAULT_TIMEOUT_MS;
    private long maxOutputBytes = DEFAULT_MAX_OUTPUT_BYTES;
    private Set<String> blockedCommands = Set.of(
        "rm -rf /",
        "mkfs",
        "dd if=/dev/zero",
        "dd if=/dev/random",
        ":(){ :|:& };:",
        "chmod -R 000 /",
        "chmod -R 777 /"
    );
    private String workDir = System.getProperty("user.dir");

    public SandboxConfig() {
    }

    public SandboxConfig(long timeoutMs, long maxOutputBytes, Set<String> blockedCommands, String workDir) {
        this.timeoutMs = timeoutMs;
        this.maxOutputBytes = maxOutputBytes;
        this.blockedCommands = blockedCommands;
        this.workDir = workDir;
    }

    public static SandboxConfig defaults() {
        return new SandboxConfig();
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public long getMaxOutputBytes() {
        return maxOutputBytes;
    }

    public void setMaxOutputBytes(long maxOutputBytes) {
        this.maxOutputBytes = maxOutputBytes;
    }

    public Set<String> getBlockedCommands() {
        return blockedCommands;
    }

    public void setBlockedCommands(Set<String> blockedCommands) {
        this.blockedCommands = blockedCommands;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }
}
