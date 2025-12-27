package com.enset.ragsecure.feature.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public GeminiService(@Value("${gemini.api.key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent")
                .build();
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        System.out.println("GeminiService initialized with API Key: " + (apiKey != null && !apiKey.isBlank()
                ? "PRESENT (Ends with: " + apiKey.substring(Math.max(0, apiKey.length() - 4)) + ")"
                : "MISSING/EMPTY"));
    }

    public String generateContent(String prompt) {
        // Simple request structure for Gemini
        // { "contents": [{ "parts": [{"text": "..."}] }] }

        var requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)))));

        String response = restClient.post()
                .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, resp) -> {
                    String responseBody = new String(resp.getBody().readAllBytes());
                    throw new RuntimeException("Gemini API Error: " + resp.getStatusCode() + " " + responseBody);
                })
                .body(String.class);

        return extractTextFromResponse(response);
    }

    private String extractTextFromResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (Exception e) {
            e.printStackTrace(); // Log error but return raw JSON as fallback to debug
            return jsonResponse;
        }
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
        // Quick and dirty manual parsing to avoid heavy logic for embeddings
        // In production, use Jackson here too if desired, but this works fine for pure
        // JSON arrays
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
