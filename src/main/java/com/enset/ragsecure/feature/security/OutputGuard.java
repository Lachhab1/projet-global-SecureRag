package com.enset.ragsecure.feature.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Advanced OutputGuard for sanitizing LLM responses
 * Protects against: Data Leakage, Hallucinations, Sensitive Information
 * Exposure
 */
@Component
public class OutputGuard {

    // Patterns for sensitive data detection
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "(?i)(password|passwd|pwd)\\s*[:=]\\s*\\S+",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern API_KEY_PATTERN = Pattern.compile(
            "(?i)(api[_-]?key|apikey|api[_-]?secret|access[_-]?key|secret[_-]?key)\\s*[:=]\\s*[\\w\\-]+",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern AWS_KEY_PATTERN = Pattern.compile(
            "(AKIA[0-9A-Z]{16})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile(
            "-----BEGIN (RSA |DSA |EC )?PRIVATE KEY-----",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern JWT_PATTERN = Pattern.compile(
            "eyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

    private static final Pattern IP_INTERNAL = Pattern.compile(
            "\\b(10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|172\\.(1[6-9]|2[0-9]|3[01])\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3})\\b");

    /**
     * Sanitizes LLM output by removing sensitive information
     * 
     * @param response Raw LLM response
     * @return Sanitized response
     */
    public String sanitize(String response) {
        if (response == null || response.isEmpty()) {
            return "";
        }

        String sanitized = response;

        // Remove credentials and keys
        sanitized = PASSWORD_PATTERN.matcher(sanitized).replaceAll("password = [REDACTED]");
        sanitized = API_KEY_PATTERN.matcher(sanitized).replaceAll("api_key = [REDACTED]");
        sanitized = AWS_KEY_PATTERN.matcher(sanitized).replaceAll("[AWS_KEY_REDACTED]");
        sanitized = PRIVATE_KEY_PATTERN.matcher(sanitized).replaceAll("[PRIVATE_KEY_REDACTED]");
        sanitized = JWT_PATTERN.matcher(sanitized).replaceAll("[JWT_TOKEN_REDACTED]");

        // Redact internal IP addresses (potential infrastructure exposure)
        sanitized = IP_INTERNAL.matcher(sanitized).replaceAll("[INTERNAL_IP_REDACTED]");

        // Detect potential hallucinations (confidence markers)
        sanitized = addHallucinationWarnings(sanitized);

        return sanitized;
    }

    /**
     * Adds warnings for potential hallucinations
     */
    private String addHallucinationWarnings(String response) {
        // Detect weak confidence phrases that may indicate hallucination
        String[] weakConfidencePhrases = {
                "I think", "I believe", "probably", "maybe", "might be",
                "could be", "it seems", "appears to be"
        };

        String lowerResponse = response.toLowerCase();
        for (String phrase : weakConfidencePhrases) {
            if (lowerResponse.contains(phrase)) {
                logSecurityEvent("HALLUCINATION_RISK", "Weak confidence detected: " + phrase);
                // Note: In production, you might want to flag this differently
                break;
            }
        }

        return response;
    }

    /**
     * Log security events for monitoring
     */
    private void logSecurityEvent(String eventType, String details) {
        System.err.println("[OUTPUT GUARD] Type: " + eventType + " | Details: " + details);
        // In production: send to monitoring system
    }
}
