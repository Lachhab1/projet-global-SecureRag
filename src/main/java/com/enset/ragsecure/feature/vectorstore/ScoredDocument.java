package com.enset.ragsecure.feature.vectorstore;

import com.enset.ragsecure.common.Document;

public record ScoredDocument(Document document, double score) {
}
