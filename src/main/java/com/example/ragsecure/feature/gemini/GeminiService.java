package com.example.ragsecure.feature.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final RestClient restClient;
    private final String apiKey;

    public GeminiService(@Value("${gemini.api.key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
                .build();
        this.apiKey = apiKey;
    }

    public String generateContent(String prompt) {
        // Simple request structure for Gemini
        // { "contents": [{ "parts": [{"text": "..."}] }] }

        var requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)))));

        return restClient.post()
                .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, response) -> {
                    String responseBody = new String(response.getBody().readAllBytes());
                    throw new RuntimeException("Gemini API Error: " + response.getStatusCode() + " " + responseBody);
                })
                .body(String.class);
    }

    public float[] getEmbedding(String text) {
        var requestBody = Map.of(
                "model", "models/text-embedding-004",
                "content", Map.of("parts", List.of(Map.of("text", text))));

        RestClient embeddingClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent")
                .build();

        String response = embeddingClient.post()
                .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return parseEmbedding(response);
    }

    private float[] parseEmbedding(String jsonResponse) {
        // Quick and dirty manual parsing to avoid strict JSON dependencies for this
        // simple base
        // In production, use Jackson's ObjectMapper
        try {
            int startIndex = jsonResponse.indexOf("\"values\": [");
            if (startIndex == -1)
                return new float[0];
            int endIndex = jsonResponse.indexOf("]", startIndex);
            String valuesStr = jsonResponse.substring(startIndex + 11, endIndex);
            String[] parts = valuesStr.split(",");
            float[] embedding = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                embedding[i] = Float.parseFloat(parts[i].trim());
            }
            return embedding;
        } catch (Exception e) {
            e.printStackTrace();
            return new float[0];
        }
    }
}
