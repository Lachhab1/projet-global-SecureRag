package com.example.ragsecure.feature.security;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InputGuard {

    private static final List<String> BLACKLIST = List.of(
            "ignore previous instructions",
            "ignore all instructions",
            "drop table",
            "system prompt",
            "you are a hacked");

    public void validate(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }

        String lowerPrompt = prompt.toLowerCase();
        for (String malicious : BLACKLIST) {
            if (lowerPrompt.contains(malicious)) {
                throw new SecurityException("Malicious prompt detected: Contains forbidden keywords.");
            }
        }
    }
}
