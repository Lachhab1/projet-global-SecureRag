package com.enset.ragsecure.common;

import java.util.Map;

/**
 * Represents a document or chunk of text to be indexed.
 */
public class Document {
    private String id;
    private String content;
    private Map<String, Object> metadata;
    private float[] embedding;

    public Document() {
    }

    public Document(String id, String content, Map<String, Object> metadata) {
        this.id = id;
        this.content = content;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
