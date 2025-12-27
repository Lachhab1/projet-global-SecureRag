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
}
