package com.example.ragsecure.feature.ingestion;

import com.example.ragsecure.common.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class CveLoader {

    public List<Document> loadMockCves() {
        List<Document> documents = new ArrayList<>();

        // Mock CVE 1: Log4Shell
        documents.add(new Document(
                UUID.randomUUID().toString(),
                "CVE-2021-44228: Apache Log4j2 JNDI features used in configuration, log messages, and parameters do not protect against attacker controlled LDAP and other JNDI related endpoints. An attacker who can control log messages or log message parameters can execute arbitrary code loaded from LDAP servers when message lookup substitution is enabled.",
                Map.of("source", "CVE-2021-44228", "severity", "CRITICAL")));

        // Mock CVE 2: Spring4Shell
        documents.add(new Document(
                UUID.randomUUID().toString(),
                "CVE-2022-22965: A Spring MVC or Spring WebFlux application running on JDK 9+ may be vulnerable to remote code execution (RCE) via data binding. The specific exploit requires the application to run on Tomcat as a WAR deployment.",
                Map.of("source", "CVE-2022-22965", "severity", "CRITICAL")));

        // Mock CVE 3: Heartbleed
        documents.add(new Document(
                UUID.randomUUID().toString(),
                "CVE-2014-0160: The (1) TLS and (2) DTLS implementations in OpenSSL 1.0.1 before 1.0.1g do not properly handle Heartbeat Extension packets, which allows remote attackers to obtain sensitive information from process memory via crafted packets that trigger a buffer over-read, as demonstrated by reading private keys, related to d1_both.c and t1_lib.c, aka the Heartbleed bug.",
                Map.of("source", "CVE-2014-0160", "severity", "HIGH")));

        return documents;
    }
}
