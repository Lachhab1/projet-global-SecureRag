package com.example.ragsecure.feature.security;

import org.springframework.stereotype.Component;

@Component
public class OutputGuard {

    public String sanitize(String response) {
        if (response == null)
            return "";

        // Simple regex to mask potential API keys or passwords (heuristic)
        // e.g., patterns like "key-12345" or "password = secret"

        // Mask AWS Keys or generic long alphanumeric strings resembling keys?
        // For atomic demo, let's mask "password" keyword if it appears in a risky
        // context.

        String sanitized = response.replaceAll("(?i)password\\s*=\\s*\\S+", "password = [REDACTED]");
        sanitized = sanitized.replaceAll("(?i)api_key\\s*=\\s*\\S+", "api_key = [REDACTED]");

        return sanitized;
    }
}
