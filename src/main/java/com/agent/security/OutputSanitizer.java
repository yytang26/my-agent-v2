package com.agent.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OutputSanitizer {

    private static final Logger log = LoggerFactory.getLogger(OutputSanitizer.class);

    // API Keys / Tokens
    private static final Pattern OPENAI_API_KEY = Pattern.compile("sk-[a-zA-Z0-9]{20,}");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("token_[a-zA-Z0-9]{10,}");
    private static final Pattern BEARER_TOKEN = Pattern.compile("Bearer\\s+[a-zA-Z0-9_\\-\\.]{20,}");
    private static final Pattern GENERIC_API_KEY = Pattern.compile("(?i)(api[_-]?key|apikey|secret[_-]?key|access[_-]?token)\\s*[:=]\\s*['\"]?([a-zA-Z0-9_\\-\\.]{10,})['\"]?");
    private static final Pattern AWS_KEY = Pattern.compile("AKIA[0-9A-Z]{16}");
    private static final Pattern PRIVATE_KEY = Pattern.compile("-----BEGIN (RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----[\\s\\S]*?-----END (RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----");
    private static final Pattern PASSWORD_IN_OUTPUT = Pattern.compile("(?i)(password|passwd|pwd|secret)\\s*[:=]\\s*['\"]?([^'\"\\s\\n]{4,})['\"]?");

    // Email addresses
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    // File paths with username
    private static final Pattern HOME_PATH = Pattern.compile("(/Users/|/home/|C:\\\\\\Users\\\\)[a-zA-Z0-9_-]+");
    private static final Pattern ABSOLUTE_HOME = Pattern.compile("/Users/[a-zA-Z0-9_-]+");

    // Environment variables with sensitive values
    private static final Pattern ENV_VAR_PASSWORD = Pattern.compile("(?i)(export\\s+)?([A-Z_]*PASSWORD[A-Z_]*|[A-Z_]*SECRET[A-Z_]*|[A-Z_]*TOKEN[A-Z_]*)\\s*=\\s*([^\\s\\n]+)");

    public String sanitize(String output) {
        if (output == null || output.isBlank()) {
            return output;
        }

        String sanitized = output;
        boolean modified = false;

        // Sanitize OpenAI API keys
        Matcher openaiMatcher = OPENAI_API_KEY.matcher(sanitized);
        if (openaiMatcher.find()) {
            sanitized = openaiMatcher.replaceAll("[REDACTED-API-KEY]");
            modified = true;
        }

        // Sanitize tokens
        Matcher tokenMatcher = TOKEN_PATTERN.matcher(sanitized);
        if (tokenMatcher.find()) {
            sanitized = tokenMatcher.replaceAll("[REDACTED-TOKEN]");
            modified = true;
        }

        // Sanitize Bearer tokens
        Matcher bearerMatcher = BEARER_TOKEN.matcher(sanitized);
        if (bearerMatcher.find()) {
            sanitized = bearerMatcher.replaceAll("Bearer [REDACTED]");
            modified = true;
        }

        // Sanitize generic API keys
        Matcher genericKeyMatcher = GENERIC_API_KEY.matcher(sanitized);
        if (genericKeyMatcher.find()) {
            sanitized = genericKeyMatcher.replaceAll(matchResult -> {
                String keyName = matchResult.group(1);
                return keyName + "=[REDACTED]";
            });
            modified = true;
        }

        // Sanitize AWS keys
        Matcher awsMatcher = AWS_KEY.matcher(sanitized);
        if (awsMatcher.find()) {
            sanitized = awsMatcher.replaceAll("[REDACTED-AWS-KEY]");
            modified = true;
        }

        // Sanitize private keys
        Matcher privateKeyMatcher = PRIVATE_KEY.matcher(sanitized);
        if (privateKeyMatcher.find()) {
            sanitized = privateKeyMatcher.replaceAll("[REDACTED-PRIVATE-KEY]");
            modified = true;
        }

        // Sanitize passwords
        Matcher passwordMatcher = PASSWORD_IN_OUTPUT.matcher(sanitized);
        if (passwordMatcher.find()) {
            sanitized = passwordMatcher.replaceAll(matchResult -> {
                String pwdName = matchResult.group(1);
                return pwdName + "=[REDACTED]";
            });
            modified = true;
        }

        // Sanitize environment variable passwords
        Matcher envMatcher = ENV_VAR_PASSWORD.matcher(sanitized);
        if (envMatcher.find()) {
            sanitized = envMatcher.replaceAll(matchResult -> {
                String prefix = matchResult.group(1) != null ? matchResult.group(1) : "";
                String varName = matchResult.group(2);
                return prefix + varName + "=[REDACTED]";
            });
            modified = true;
        }

        // Mask email addresses (partial)
        Matcher emailMatcher = EMAIL_PATTERN.matcher(sanitized);
        if (emailMatcher.find()) {
            sanitized = emailMatcher.replaceAll(matchResult -> {
                String email = matchResult.group();
                int atIndex = email.indexOf('@');
                if (atIndex > 1) {
                    String local = email.substring(0, atIndex);
                    String domain = email.substring(atIndex);
                    if (local.length() <= 2) {
                        return "***" + domain;
                    }
                    return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
                }
                return "[REDACTED-EMAIL]";
            });
            modified = true;
        }

        // Replace username in paths with ~
        Matcher homeMatcher = HOME_PATH.matcher(sanitized);
        if (homeMatcher.find()) {
            sanitized = homeMatcher.replaceAll(matchResult -> {
                String path = matchResult.group();
                if (path.startsWith("/Users/")) {
                    return "/Users/~";
                } else if (path.startsWith("/home/")) {
                    return "/home/~";
                } else if (path.startsWith("C:\\\\")) {
                    return "C:\\\\Users\\\\~";
                }
                return path;
            });
            modified = true;
        }

        if (modified) {
            log.debug("Output sanitized: sensitive information redacted");
        }

        return sanitized;
    }
}
