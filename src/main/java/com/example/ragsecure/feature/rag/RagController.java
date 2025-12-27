package com.example.ragsecure.feature.rag;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public Object ask(@RequestBody Map<String, String> payload) {
        String query = payload.get("query");
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query is required");
        }

        try {
            return ragService.ask(query);
        } catch (SecurityException e) {
            return Map.of("error", "Security Alert: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Error processing request: " + e.getMessage());
        }
    }
}
