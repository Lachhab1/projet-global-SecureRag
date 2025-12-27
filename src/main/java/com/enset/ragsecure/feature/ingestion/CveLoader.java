package com.enset.ragsecure.feature.ingestion;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.enset.ragsecure.common.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class CveLoader {

        private final RestClient restClient;
        private final ObjectMapper objectMapper;

        public CveLoader() {
                this.restClient = RestClient.builder()
                                .baseUrl("https://cve.circl.lu/api")
                                .build();
                this.objectMapper = new ObjectMapper();
        }

        public List<Document> loadMockCves() {
                System.out.println("Fetching real CVE data from cve.circl.lu...");

                try {
                        // Fetch last 10 updated CVEs from public API
                        String response = restClient.get()
                                        .uri("/last/10")
                                        .retrieve()
                                        .body(String.class);

                        return parseCveResponse(response);
                } catch (Exception e) {
                        System.err.println("Failed to fetch CVEs from API: " + e.getMessage());
                        System.out.println("Falling back to static data...");
                        return loadFallbackCves();
                }
        }

        private List<Document> parseCveResponse(String jsonResponse) throws Exception {
                List<Document> documents = new ArrayList<>();
                JsonNode root = objectMapper.readTree(jsonResponse);

                int count = 0;
                for (JsonNode cveNode : root) {
                        if (count >= 7)
                                break; // Limit to 7 to avoid too many API calls

                        // CVE 5.0 format: nested structure
                        String cveId = cveNode.path("cveMetadata").path("cveId").asText("");

                        // Extract description from containers.cna.descriptions array
                        String summary = "";
                        JsonNode descriptions = cveNode.path("containers").path("cna").path("descriptions");
                        if (descriptions.isArray() && descriptions.size() > 0) {
                                summary = descriptions.get(0).path("value").asText("");
                        }

                        // Extract CVSS score if available
                        String severity = "UNKNOWN";
                        JsonNode adp = cveNode.path("containers").path("adp");
                        if (adp.isArray() && adp.size() > 0) {
                                JsonNode metrics = adp.get(0).path("metrics");
                                if (metrics.isArray() && metrics.size() > 0) {
                                        double baseScore = metrics.get(0).path("cvssV3_1").path("baseScore")
                                                        .asDouble(0);
                                        severity = baseScore >= 9.0 ? "CRITICAL"
                                                        : baseScore >= 7.0 ? "HIGH"
                                                                        : baseScore >= 4.0 ? "MEDIUM" : "LOW";
                                }
                        }

                        // Only use if we have meaningful data
                        if (!cveId.isEmpty() && !summary.isEmpty() && summary.length() > 50) {
                                documents.add(new Document(
                                                UUID.randomUUID().toString(),
                                                cveId + ": " + summary,
                                                Map.of("source", cveId, "severity", severity)));
                                count++;
                        }
                }

                if (documents.isEmpty()) {
                        return loadFallbackCves();
                }

                System.out.println("Successfully loaded " + documents.size() + " real CVEs from API");
                return documents;
        }

        private List<Document> loadFallbackCves() {
                List<Document> documents = new ArrayList<>();

                // Fallback: Real recent CVEs (static)
                documents.add(new Document(
                                UUID.randomUUID().toString(),
                                "CVE-2023-44487: The HTTP/2 protocol allows a denial of service (server resource consumption) because request cancellation can reset many streams quickly, as exploited in the wild in August through October 2023. This is known as the 'HTTP/2 Rapid Reset' attack.",
                                Map.of("source", "CVE-2023-44487", "severity", "CRITICAL")));

                documents.add(new Document(
                                UUID.randomUUID().toString(),
                                "CVE-2024-3094: Malicious code was discovered in the upstream tarballs of xz, starting with version 5.6.0. The liblzma build process extracts a prebuilt object file from a disguised test file, which is used to modify specific functions in the liblzma code, intercepting and modifying data interaction.",
                                Map.of("source", "CVE-2024-3094", "severity", "CRITICAL")));

                documents.add(new Document(
                                UUID.randomUUID().toString(),
                                "CVE-2021-44228: Apache Log4j2 JNDI features used in configuration, log messages, and parameters do not protect against attacker controlled LDAP and other JNDI related endpoints. An attacker who can control log messages can execute arbitrary code loaded from LDAP servers when message lookup substitution is enabled.",
                                Map.of("source", "CVE-2021-44228", "severity", "CRITICAL")));

                documents.add(new Document(
                                UUID.randomUUID().toString(),
                                "CVE-2022-22965: A Spring MVC or Spring WebFlux application running on JDK 9+ may be vulnerable to remote code execution (RCE) via data binding. The specific exploit requires the application to run on Tomcat as a WAR deployment.",
                                Map.of("source", "CVE-2022-22965", "severity", "CRITICAL")));

                documents.add(new Document(
                                UUID.randomUUID().toString(),
                                "CVE-2023-38545: This flaw makes curl overflow a heap based buffer in the SOCKS5 proxy handshake. When curl is asked to use SOCKS5, the hostname is passed to the proxy instead of being resolved by curl itself. If the hostname is longer than 255 bytes, the target buffer is too small.",
                                Map.of("source", "CVE-2023-38545", "severity", "HIGH")));

                return documents;
        }
}
