package com.example.ragsecure.feature.vectorstore;

import com.example.ragsecure.common.Document;

public record ScoredDocument(Document document, double score) {
}
