# Project RAG Secure

## Overview
This is a Spring Boot application integrated with Google Gemini API.
The project is designed with **atomic feature architecture** to support concurrent development by a team of 5 developers.

## Team Development Guidelines

### 1. Atomic Feature Structure
To minimize merge conflicts and ensure modularity, we follow a feature-driven package structure:

```
com.example.ragsecure
├── common          // Shared utilities, exceptions, and global constants
├── config          // Global Spring configurations (e.g., AppConfig, Security)
├── feature
│   ├── gemini      // Gemini API integration (Service, Controller, Models)
│   ├── [featureX]  // Future feature (e.g., Ingestion)
│   └── [featureY]  // Future feature (e.g., Search)
└── RagSecureApplication.java
```

**Rules:**
1.  **Isolation**: Each sub-package under `feature` should be self-contained. Avoid cross-feature dependencies if possible; communicate via `common` interfaces or events.
2.  **Responsibility**: One developer (or pair) owns a feature package.
3.  **Encapsulation**: Keep classes package-private if they don't need to be exposed outside the feature.

### 2. Getting Started

#### Prerequisites
- Java 17+
- Maven (use `./mvnw` wrapper)
- Gemini API Key

#### Configuration
Set your environment variable for the API key:
```bash
export GEMINI_API_KEY="your_api_key_here"
```

#### Running the App
```bash
./mvnw spring-boot:run
```

#### Testing
Check the health or chat endpoint:
```bash
curl -X POST http://localhost:8080/api/gemini/chat \
     -H "Content-Type: application/json" \
     -d '{"prompt": "Hello Gemini"}'
```

## Features

### Gemini Integration
- **Package**: `feature.gemini`
- **Service**: `GeminiService` - wrappers for Gemini API.
- **Controller**: `GeminiController` - REST endpoints.
