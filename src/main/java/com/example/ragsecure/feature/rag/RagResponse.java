package com.example.ragsecure.feature.rag;

import java.util.List;

public record RagResponse(
        String answer,
        String confidence,
        List<String> sources) {
}
