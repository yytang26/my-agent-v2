package com.agent.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class InjectionDetector {

    private static final Logger log = LoggerFactory.getLogger(InjectionDetector.class);

    private final boolean enabled;
    private static final int MAX_INPUT_LENGTH = 10000;

    private final List<Pattern> injectionPatterns = List.of(
        // English prompt injection patterns
        Pattern.compile("ignore\\s+(previous|all|the)\\s+instruction[s]?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("disregard\\s+(previous|all|the)\\s+instruction[s]?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("you\\s+are\\s+now\\s+(a\\s+)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("forget\\s+(previous|all|the)\\s+instruction[s]?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("system\\s+prompt", Pattern.CASE_INSENSITIVE),
        Pattern.compile("reveal\\s+your\\s+(system\\s+)?prompt", Pattern.CASE_INSENSITIVE),
        Pattern.compile("show\\s+(me\\s+)?your\\s+instruction[s]?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(DAN|do\\s+anything\\s+now)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("jailbreak", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(new|different)\\s+persona", Pattern.CASE_INSENSITIVE),

        // Chinese prompt injection patterns
        Pattern.compile("忽略(之前|所有|上述)的?指令", Pattern.CASE_INSENSITIVE),
        Pattern.compile("你现在是", Pattern.CASE_INSENSITIVE),
        Pattern.compile("忘记(之前|所有|上述)的?指令", Pattern.CASE_INSENSITIVE),
        Pattern.compile("系统提示", Pattern.CASE_INSENSITIVE),
        Pattern.compile("揭示你的(系统)?提示", Pattern.CASE_INSENSITIVE),
        Pattern.compile("展示你的指令", Pattern.CASE_INSENSITIVE),
        Pattern.compile("越狱", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(新的|不同的)人格", Pattern.CASE_INSENSITIVE),

        // Delimiter-based attacks
        Pattern.compile("<\\|system\\|>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\[SYSTEM\\]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\[INSTRUCTION\\]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<<<\\s*SYSTEM", Pattern.CASE_INSENSITIVE),

        // Role override
        Pattern.compile("from\\s+now\\s+on\\s+you\\s+are", Pattern.CASE_INSENSITIVE),
        Pattern.compile("从现在开始你是", Pattern.CASE_INSENSITIVE),
        Pattern.compile("act\\s+as\\s+(if\\s+you\\s+are)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("扮演", Pattern.CASE_INSENSITIVE)
    );

    private final Pattern base64Pattern = Pattern.compile("^[A-Za-z0-9+/]{50,}={0,2}$");
    private final Pattern mixedScriptPattern = Pattern.compile("[\\u4e00-\\u9fff].*[a-zA-Z]{20,}|[a-zA-Z]{20,}.*[\\u4e00-\\u9fff]");

    public InjectionDetector(@Value("${agent.security.injection-detection:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public InjectionResult detect(String input) {
        if (!enabled || input == null || input.isBlank()) {
            return InjectionResult.clean();
        }

        String lowerInput = input.toLowerCase();

        // Check 1: Input length
        if (input.length() > MAX_INPUT_LENGTH) {
            log.warn("Injection detected: input exceeds max length ({} > {})", input.length(), MAX_INPUT_LENGTH);
            return new InjectionResult(true, "EXCESSIVE_LENGTH", 0.6,
                "Input exceeds " + MAX_INPUT_LENGTH + " characters");
        }

        // Check 2: Pattern matching
        for (Pattern pattern : injectionPatterns) {
            if (pattern.matcher(input).find()) {
                String patternName = extractPatternName(pattern);
                log.warn("Injection detected: matched pattern '{}'", patternName);
                return new InjectionResult(true, patternName, 0.85,
                    "Potential prompt injection attack detected: " + patternName);
            }
        }

        // Check 3: Base64 encoded hidden instructions
        String[] words = input.split("\\s+");
        for (String word : words) {
            String trimmed = word.trim();
            if (trimmed.length() >= 50 && base64Pattern.matcher(trimmed).matches()) {
                try {
                    String decoded = new String(Base64.getDecoder().decode(trimmed));
                    // Check if decoded content contains suspicious keywords
                    if (containsSuspiciousKeywords(decoded)) {
                        log.warn("Injection detected: Base64-encoded hidden instructions");
                        return new InjectionResult(true, "BASE64_HIDDEN", 0.9,
                            "Base64-encoded hidden instructions detected");
                    }
                } catch (IllegalArgumentException e) {
                    // Not valid base64, ignore
                }
            }
        }

        // Check 4: Mixed script injection (multilingual mixing)
        if (mixedScriptPattern.matcher(input).find()) {
            log.warn("Injection detected: mixed script/multilingual injection");
            return new InjectionResult(true, "MIXED_SCRIPT", 0.5,
                "Multilingual mixed content detected, potential injection vector");
        }

        return InjectionResult.clean();
    }

    private String extractPatternName(Pattern pattern) {
        String regex = pattern.pattern();
        // Simplify regex to a readable name
        if (regex.contains("ignore") || regex.contains("忽略")) {
            return "IGNORE_INSTRUCTIONS";
        }
        if (regex.contains("you are now") || regex.contains("你现在是") || regex.contains("role") || regex.contains("人格")) {
            return "ROLE_HIJACKING";
        }
        if (regex.contains("system prompt") || regex.contains("系统提示")) {
            return "SYSTEM_PROMPT_EXTRACTION";
        }
        if (regex.contains("jailbreak") || regex.contains("越狱")) {
            return "JAILBREAK_ATTEMPT";
        }
        if (regex.contains("DAN") || regex.contains("anything now")) {
            return "DAN_ATTACK";
        }
        if (regex.contains("<|system|>") || regex.contains("[SYSTEM]") || regex.contains("<<<")) {
            return "DELIMITER_INJECTION";
        }
        if (regex.contains("act as") || regex.contains("扮演")) {
            return "ROLE_PLAY_INJECTION";
        }
        return "UNKNOWN_PATTERN";
    }

    private boolean containsSuspiciousKeywords(String decoded) {
        String lower = decoded.toLowerCase();
        String[] suspicious = {
            "ignore", "instruction", "system", "prompt", "forget",
            "忽略", "指令", "系统", "提示", "忘记"
        };
        for (String keyword : suspicious) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
