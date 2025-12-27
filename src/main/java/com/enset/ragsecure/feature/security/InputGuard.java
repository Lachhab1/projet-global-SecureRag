package com.enset.ragsecure.feature.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Advanced InputGuard with multi-layered prompt injection detection
 * Protects against: Prompt Injection, Jailbreak Attacks, Command Injection
 */
@Component
public class InputGuard {

    // Layer 1: Explicit malicious keyword blacklist
    private static final List<String> BLACKLIST_KEYWORDS = List.of(
            // Prompt Override Attacks
            "ignore previous instructions",
            "ignore all instructions",
            "ignore above",
            "disregard previous",
            "forget everything",
            "new instructions",
            "override instructions",

            // System Manipulation
            "you are now",
            "you are a hacked",
            "system prompt",
            "reveal your prompt",
            "show your instructions",
            "what are your guidelines",

            // Jailbreak Attempts
            "pretend you are",
            "roleplay as",
            "act as if",
            "simulate being",
            "hypothetically",

            // Command/SQL Injection
            "drop table",
            "delete from",
            "insert into",
            "'; drop",
            "union select",
            "exec(",
            "execute(",

            // Encoding/Obfuscation Detection
            "base64",
            "hex encode",
            "rot13",
            "url encode");

    // Layer 2: Regex patterns for sophisticated attacks
    private static final List<Pattern> MALICIOUS_PATTERNS = List.of(
            // Instruction Override Patterns
            Pattern.compile(
                    "(?i)\\b(ignore|disregard|forget)\\s+(all|previous|above|prior)\\s+(instructions?|rules?|guidelines?|commands?)"),
            Pattern.compile("(?i)\\b(new|updated|revised)\\s+(instructions?|rules?|system\\s+prompt)"),

            // Role Manipulation
            Pattern.compile("(?i)(you\\s+are\\s+now|act\\s+as|pretend\\s+to\\s+be|simulate\\s+being)\\s+\\w+"),
            Pattern.compile("(?i)(jailbreak|dan\\s+mode|developer\\s+mode|god\\s+mode)"),

            // Prompt Leakage Attempts
            Pattern.compile(
                    "(?i)(show|reveal|display|print|output)\\s+(your|the)\\s+(prompt|instructions?|system\\s+message)"),

            // SQL/Command Injection Signatures
            Pattern.compile("(?i)(drop|delete|insert|update)\\s+(table|from|into)"),
            Pattern.compile("(?i)(union|concat)\\s+select"),
            Pattern.compile("(?i);\\s*(drop|delete|exec)"),

            // Suspicious Character Patterns (potential encoding)
            Pattern.compile("(%[0-9A-Fa-f]{2}){5,}"), // URL encoded sequences
            Pattern.compile("(\\\\x[0-9A-Fa-f]{2}){5,}"), // Hex escape sequences
            Pattern.compile("([A-Za-z0-9+/]{20,}={0,2})") // Base64-like strings (20+ chars)
    );

    // Layer 3: Structural Validation
    private static final int MAX_PROMPT_LENGTH = 2000;
    private static final int MAX_SPECIAL_CHARS_RATIO = 30; // 30% max special characters

    /**
     * Validates input prompt using multi-layer security checks
     * 
     * @param prompt User input to validate
     * @throws SecurityException        if malicious content detected
     * @throws IllegalArgumentException if input is invalid
     */
    public void validate(String prompt) {
        // Basic validation
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }

        // Length validation (prevent resource exhaustion)
        if (prompt.length() > MAX_PROMPT_LENGTH) {
            throw new SecurityException("Prompt exceeds maximum length (potential attack)");
        }

        String lowerPrompt = prompt.toLowerCase();

        // Layer 1: Blacklist keyword detection
        for (String malicious : BLACKLIST_KEYWORDS) {
            if (lowerPrompt.contains(malicious)) {
                logSecurityEvent("KEYWORD_BLACKLIST", malicious);
                throw new SecurityException("Forbidden keyword detected");
            }
        }

        // Layer 2: Regex pattern matching for sophisticated attacks
        for (Pattern pattern : MALICIOUS_PATTERNS) {
            if (pattern.matcher(prompt).find()) {
                logSecurityEvent("PATTERN_MATCH", pattern.pattern());
                throw new SecurityException("Suspicious pattern detected");
            }
        }

        // Layer 3: Structural Analysis
        validateStructure(prompt);
    }

    /**
     * Analyzes prompt structure for anomalies
     */
    private void validateStructure(String prompt) {
        // Check for excessive special characters (potential encoding/obfuscation)
        long specialCharCount = prompt.chars()
                .filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
                .count();

        int specialCharPercentage = (int) ((specialCharCount * 100.0) / prompt.length());

        if (specialCharPercentage > MAX_SPECIAL_CHARS_RATIO) {
            logSecurityEvent("SPECIAL_CHARS", String.valueOf(specialCharPercentage) + "%");
            throw new SecurityException("Excessive special characters detected (potential encoding attack)");
        }

        // Check for suspicious repetition (potential buffer overflow or fuzzing)
        if (hasExcessiveRepetition(prompt)) {
            logSecurityEvent("REPETITION", "Excessive character repetition");
            throw new SecurityException("Suspicious input pattern detected");
        }
    }

    /**
     * Detects excessive character repetition
     */
    private boolean hasExcessiveRepetition(String prompt) {
        // Check for 10+ consecutive identical characters
        return Pattern.compile("(.)\\1{9,}").matcher(prompt).find();
    }

    /**
     * Log security events for monitoring and analysis
     */
    private void logSecurityEvent(String detectionType, String details) {
        System.err.println("[SECURITY ALERT] Type: " + detectionType + " | Details: " + details);
        // In production: send to SIEM, logging service, or security dashboard
    }
}
