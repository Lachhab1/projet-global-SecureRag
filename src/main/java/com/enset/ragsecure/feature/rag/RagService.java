package com.enset.ragsecure.feature.rag;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import com.enset.ragsecure.common.Document;
import com.enset.ragsecure.feature.gemini.GeminiService;
import com.enset.ragsecure.feature.ingestion.CveLoader;
import com.enset.ragsecure.feature.ingestion.MitreLoader;
import com.enset.ragsecure.feature.security.InputGuard;
import com.enset.ragsecure.feature.security.OutputGuard;
import com.enset.ragsecure.feature.vectorstore.VectorService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final GeminiService geminiService;
    private final VectorService vectorService;
    private final CveLoader cveLoader;
    private final MitreLoader mitreLoader; // New Loader
    private final InputGuard inputGuard;
    private final OutputGuard outputGuard;

    public RagService(GeminiService geminiService, VectorService vectorService, CveLoader cveLoader,
            MitreLoader mitreLoader, InputGuard inputGuard, OutputGuard outputGuard) {
        this.geminiService = geminiService;
        this.vectorService = vectorService;
        this.cveLoader = cveLoader;
        this.mitreLoader = mitreLoader;
        this.inputGuard = inputGuard;
        this.outputGuard = outputGuard;
    }

    @PostConstruct
    public void init() {
        System.out.println("Initializing RAG Pipeline: Loading and Embedding Data...");

        // Load Real CVEs and Real MITRE Techniques
        List<Document> docs = new java.util.ArrayList<>();
        docs.addAll(cveLoader.loadMockCves());
        docs.addAll(mitreLoader.loadMitreTechniques());

        for (Document doc : docs) {
            try {
                // Add a small delay to avoid hitting Gemini rate limits (429) on startup
                Thread.sleep(1500); // 1.5 seconds delay
                float[] embedding = geminiService.getEmbedding(doc.getContent());
                doc.setEmbedding(embedding);
                vectorService.addDocument(doc);
            } catch (Exception e) {
                System.err.println("Failed to embed document: " + doc.getId() + ". Error: " + e.getMessage());
                // Continue to next document so app doesn't crash completely
            }
        }
        System.out.println("RAG Pipeline Initialized with " + docs.size() + " documents (CVEs + MITRE).");
    }

    public RagResponse ask(String query) {
        // 1. Security: Input Validation
        inputGuard.validate(query);

        // 2. Retrieval
        float[] queryEmbedding = geminiService.getEmbedding(query);
        List<com.enset.ragsecure.feature.vectorstore.ScoredDocument> scoredDocs = vectorService.search(queryEmbedding,
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
