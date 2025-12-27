package com.enset.ragsecure.feature.vectorstore;

import org.springframework.stereotype.Service;

import com.enset.ragsecure.common.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SimpleVectorStore implements VectorService {

    private final Map<String, Document> store = new ConcurrentHashMap<>();

    @Override
    public void addDocument(Document document) {
        store.put(document.getId(), document);
    }

    @Override
    public List<ScoredDocument> search(float[] queryVector, int topK) {
        if (queryVector == null || queryVector.length == 0)
            return Collections.emptyList();

        return store.values().stream()
                .filter(doc -> doc.getEmbedding() != null)
                .map(doc -> new ScoredDocument(doc, cosineSimilarity(queryVector, doc.getEmbedding())))
                .sorted(Comparator.comparingDouble(ScoredDocument::score).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    private double cosineSimilarity(float[] v1, float[] v2) {
        if (v1.length != v2.length)
            return 0.0;
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            normA += v1[i] * v1[i];
            normB += v2[i] * v2[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
