package com.enset.ragsecure.feature.vectorstore;

import java.util.List;

import com.enset.ragsecure.common.Document;

public interface VectorService {
    void addDocument(Document document);

    List<ScoredDocument> search(float[] queryVector, int topK);
}
