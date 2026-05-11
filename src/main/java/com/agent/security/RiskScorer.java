package com.agent.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class RiskScorer {

    private static final Logger log = LoggerFactory.getLogger(RiskScorer.class);

    // Read-only tools -> LOW
    private static final Set<String> READ_ONLY_TOOLS = Set.of(
        "read_file", "list_directory", "grep", "glob", "get_current_time"
    );

    // Write tools -> MEDIUM
    private static final Set<String> WRITE_TOOLS = Set.of(
        "write_file", "edit_file"
    );

    // Bash read-only commands
    private static final Set<String> BASH_READ_ONLY = Set.of(
        "ls", "cat", "head", "tail", "less", "more", "find", "grep", "awk", "sed", "sort",
        "wc", "diff", "echo", "pwd", "whoami", "date", "which", "whereis",
        "mvn", "gradle", "npm", "node", "python", "python3", "java", "javac",
        "git", "git status", "git log", "git diff", "git branch", "git show"
    );

    // Bash modification commands -> MEDIUM
    private static final Set<String> BASH_MODIFY = Set.of(
        "mkdir", "cp", "mv", "rename", "touch", "chmod", "chown", "ln"
    );

    // Dangerous commands -> HIGH
    private static final Set<String> BASH_DANGEROUS = Set.of(
        "rm", "rmdir", "curl", "wget", "sudo", "su", "passwd", "useradd", "usermod",
        "kill", "killall", "pkill", "shutdown", "reboot", "poweroff", "halt",
        "mount", "umount", "fdisk", "parted", "mkfs", "format", "fsck",
        "iptables", "ufw", "firewalld", "nc", "netcat", "nmap", "telnet",
        "ssh", "scp", "sftp", "ftp", "tftp",
        "base64", "xxd", "openssl", "gpg",
        "eval", "exec", "source", ".",
        "docker", "kubectl", "helm"
    );

    // Critical patterns
    private static final List<Pattern> CRITICAL_PATTERNS = List.of(
        Pattern.compile("rm\\s+-rf\\s+/"),
        Pattern.compile("rm\\s+-rf\\s+/\\.*"),
        Pattern.compile(":\\(\\)\\{\\s*:\\s*\\|\\s*:\\s*&\\s*\\}\\s*;\\s*:"),
        Pattern.compile("dd\\s+if=/dev/zero\\s+of=/dev/"),
        Pattern.compile("dd\\s+if=/dev/random\\s+of=/dev/"),
        Pattern.compile("mkfs\\.?[a-z0-9]*\\s+/dev/"),
        Pattern.compile(">\\s*/dev/"),
        Pattern.compile("chmod\\s+-R\\s+777\\s+/"),
        Pattern.compile("chmod\\s+-R\\s+000\\s+/"),
        Pattern.compile("curl\\s+.*\\s*\\|\\s*(sh|bash|zsh)"),
        Pattern.compile("wget\\s+.*\\s*\\|\\s*(sh|bash|zsh)"),
        Pattern.compile("python\\s+-c\\s+.*import\\s+os"),
        Pattern.compile("python3\\s+-c\\s+.*import\\s+os"),
        Pattern.compile("perl\\s+-e\\s+.*system"),
        Pattern.compile("ruby\\s+-e\\s+.*system"),
        Pattern.compile("nc\\s+-[el]"),
        Pattern.compile("bash\\s+-i\\s+>&\\s+/dev/tcp/")
    );

    public RiskAssessment assess(String toolName, Map<String, Object> arguments) {
        if (toolName == null) {
            return RiskAssessment.medium("Unknown tool (null name)");
        }

        // Read-only tools
        if (READ_ONLY_TOOLS.contains(toolName)) {
            return RiskAssessment.low("Read-only operation: " + toolName);
        }

        // Write tools
        if (WRITE_TOOLS.contains(toolName)) {
            return RiskAssessment.medium("File write operation: " + toolName);
        }

        // Delegate task
        if ("delegate_task".equals(toolName)) {
            return RiskAssessment.medium("Task delegation operation");
        }

        // Bash command analysis
        if ("bash".equals(toolName)) {
            return assessBashCommand(arguments);
        }

        // Default for unknown tools
        return RiskAssessment.medium("Unknown tool: " + toolName);
    }

    private RiskAssessment assessBashCommand(Map<String, Object> arguments) {
        String command = "";
        if (arguments != null && arguments.containsKey("command")) {
            Object cmdObj = arguments.get("command");
            command = cmdObj != null ? cmdObj.toString() : "";
        }

        if (command.isBlank()) {
            return RiskAssessment.medium("Empty bash command");
        }

        String lowerCmd = command.toLowerCase().trim();

        // Check critical patterns first
        for (Pattern pattern : CRITICAL_PATTERNS) {
            if (pattern.matcher(command).find()) {
                log.warn("Critical bash command detected: {}", command);
                return RiskAssessment.critical("Critical dangerous command: " + pattern.pattern());
            }
        }

        // Check for dangerous commands
        String[] cmdParts = lowerCmd.split("\\s+|;|\\||&&|\\$\\(");
        for (String part : cmdParts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            String cmd = part;
            // Remove leading path
            if (cmd.contains("/")) {
                cmd = cmd.substring(cmd.lastIndexOf('/') + 1);
            }

            if (BASH_DANGEROUS.contains(cmd)) {
                log.warn("Dangerous bash command detected: {}", cmd);
                return RiskAssessment.high("Dangerous command detected: " + cmd);
            }
        }

        // Check for modification commands
        for (String part : cmdParts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            String cmd = part;
            if (cmd.contains("/")) {
                cmd = cmd.substring(cmd.lastIndexOf('/') + 1);
            }
            if (BASH_MODIFY.contains(cmd)) {
                return RiskAssessment.medium("File modification command: " + cmd);
            }
        }

        // Check for read-only commands
        for (String part : cmdParts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            String cmd = part;
            if (cmd.contains("/")) {
                cmd = cmd.substring(cmd.lastIndexOf('/') + 1);
            }
            if (BASH_READ_ONLY.contains(cmd)) {
                return RiskAssessment.low("Read-only command: " + cmd);
            }
        }

        // Unknown bash command -> MEDIUM as default
        return RiskAssessment.medium("Unclassified bash command");
    }
}
