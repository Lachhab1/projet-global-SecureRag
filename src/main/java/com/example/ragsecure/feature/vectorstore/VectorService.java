package com.example.ragsecure.feature.vectorstore;

import com.example.ragsecure.common.Document;
import java.util.List;

public interface VectorService {
    void addDocument(Document document);

    List<ScoredDocument> search(float[] queryVector, int topK);
}
