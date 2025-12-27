package com.enset.ragsecure.feature.ingestion;

import com.enset.ragsecure.common.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class MitreLoader {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MitreLoader() {
        this.restClient = RestClient.builder()
                .baseUrl("https://raw.githubusercontent.com")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<Document> loadMitreTechniques() {
        System.out.println("Fetching MITRE ATT&CK data from GitHub...");

        try {
            // Fetch MITRE Enterprise ATT&CK JSON
            String response = restClient.get()
                    .uri("/mitre-attack/attack-stix-data/master/enterprise-attack/enterprise-attack-15.1.json")
                    .retrieve()
                    .body(String.class);

            return parseMitreResponse(response);
        } catch (Exception e) {
            System.err.println("Failed to fetch MITRE data: " + e.getMessage());
            System.out.println("Falling back to static MITRE data...");
            return loadFallbackMitre();
        }
    }

    private List<Document> parseMitreResponse(String jsonResponse) throws Exception {
        List<Document> documents = new ArrayList<>();
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode objects = root.path("objects");

        int count = 0;
        for (JsonNode obj : objects) {
            if (count >= 5)
                break; // Limit to 5 techniques

            String type = obj.path("type").asText();
            if (!"attack-pattern".equals(type))
                continue;

            String techniqueId = "";
            JsonNode externalRefs = obj.path("external_references");
            for (JsonNode ref : externalRefs) {
                if ("mitre-attack".equals(ref.path("source_name").asText())) {
                    techniqueId = ref.path("external_id").asText();
                    break;
                }
            }

            String name = obj.path("name").asText();
            String description = obj.path("description").asText();

            if (!techniqueId.isEmpty() && !description.isEmpty() && techniqueId.startsWith("T")) {
                documents.add(new Document(
                        UUID.randomUUID().toString(),
                        "MITRE " + techniqueId + " - " + name + ": "
                                + description.substring(0, Math.min(500, description.length())),
                        Map.of("source", "MITRE ATT&CK " + techniqueId, "type", "technique")));
                count++;
            }
        }

        if (documents.isEmpty()) {
            return loadFallbackMitre();
        }

        System.out.println("Successfully loaded " + documents.size() + " MITRE techniques from API");
        return documents;
    }

    private List<Document> loadFallbackMitre() {
        List<Document> documents = new ArrayList<>();

        documents.add(new Document(
                UUID.randomUUID().toString(),
                "MITRE T1190 - Exploit Public-Facing Application: Adversaries may attempt to exploit weaknesses in Internet-facing applications using software vulnerabilities, configuration errors, or by leveraging specific application features to cause unintended behavior.",
                Map.of("source", "MITRE ATT&CK T1190", "type", "initial-access")));

        documents.add(new Document(
                UUID.randomUUID().toString(),
                "MITRE T1059 - Command and Scripting Interpreter: Adversaries may abuse command and script interpreters to execute commands, scripts, or binaries. These interfaces provide ways to interact with computer systems and are common across many platforms.",
                Map.of("source", "MITRE ATT&CK T1059", "type", "execution")));

        documents.add(new Document(
                UUID.randomUUID().toString(),
                "MITRE T1566 - Phishing: Adversaries may send phishing messages to gain access to victim systems. All forms of phishing are electronically delivered social engineering targeting users to induce them to perform specific actions.",
                Map.of("source", "MITRE ATT&CK T1566", "type", "initial-access")));

        documents.add(new Document(
                UUID.randomUUID().toString(),
                "MITRE T1003 - OS Credential Dumping: Adversaries may attempt to dump credentials to obtain account login and credential material from operating system memory, credential stores, or authentication modules.",
                Map.of("source", "MITRE ATT&CK T1003", "type", "credential-access")));

        documents.add(new Document(
                UUID.randomUUID().toString(),
                "MITRE T1486 - Data Encrypted for Impact: Adversaries may encrypt data on target systems to interrupt availability to system and network resources. The adversary may render stored data inaccessible by encrypting files or withholding access to a decryption key (ransomware).",
                Map.of("source", "MITRE ATT&CK T1486", "type", "impact")));

        return documents;
    }
}
