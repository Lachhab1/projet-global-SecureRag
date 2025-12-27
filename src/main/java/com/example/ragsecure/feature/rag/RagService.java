package com.example.ragsecure.feature.rag;

import com.example.ragsecure.common.Document;
import com.example.ragsecure.feature.gemini.GeminiService;
import com.example.ragsecure.feature.ingestion.CveLoader;
import com.example.ragsecure.feature.security.InputGuard;
import com.example.ragsecure.feature.security.OutputGuard;
import com.example.ragsecure.feature.vectorstore.VectorService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final GeminiService geminiService;
    private final VectorService vectorService;
    private final CveLoader cveLoader;
    private final InputGuard inputGuard;
    private final OutputGuard outputGuard;

    public RagService(GeminiService geminiService, VectorService vectorService, CveLoader cveLoader,
            InputGuard inputGuard, OutputGuard outputGuard) {
        this.geminiService = geminiService;
        this.vectorService = vectorService;
        this.cveLoader = cveLoader;
        this.inputGuard = inputGuard;
        this.outputGuard = outputGuard;
    }

    @PostConstruct
    public void init() {
        System.out.println("Initializing RAG Pipeline: Loading and Embedding Data...");
        List<Document> docs = cveLoader.loadMockCves();
        for (Document doc : docs) {
            float[] embedding = geminiService.getEmbedding(doc.getContent());
            doc.setEmbedding(embedding);
            vectorService.addDocument(doc);
        }
        System.out.println("RAG Pipeline Initialized with " + docs.size() + " documents.");
    }

    public RagResponse ask(String query) {
        // 1. Security: Input Validation
        inputGuard.validate(query);

        // 2. Retrieval
        float[] queryEmbedding = geminiService.getEmbedding(query);
        List<com.example.ragsecure.feature.vectorstore.ScoredDocument> scoredDocs = vectorService.search(queryEmbedding,
                2); // Top 2 chunks

        String context = scoredDocs.stream()
                .map(sd -> sd.document().getContent())
                .collect(Collectors.joining("\n\n"));

        List<String> sources = scoredDocs.stream()
                .map(sd -> sd.document().getMetadata().getOrDefault("source", "Unknown").toString())
                .distinct()
                .collect(Collectors.toList());

        // Confidence Logic (Bonus)
        double topScore = scoredDocs.isEmpty() ? 0.0 : scoredDocs.get(0).score();
        String confidence = topScore > 0.75 ? "High" : (topScore > 0.5 ? "Medium" : "Low");

        // 3. Augmentation
        String prompt = "You are a Cyber Threat Intelligence Analyst. Use the following context to answer the question.\n"
                +
                "Context:\n" + context + "\n\n" +
                "Question: " + query;

        // 4. Generation
        String rawResponse = geminiService.generateContent(prompt);

        // 5. Security: Output Sanitization
        String sanitizedResponse = outputGuard.sanitize(rawResponse);

        return new RagResponse(sanitizedResponse, confidence, sources);
    }
}
