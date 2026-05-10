package com.agent.rag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Chunk {

    private final String id;
    private String content;
    private String sourceFile;
    private int startLine;
    private int endLine;
    private Map<String, String> metadata;
    private double[] embedding;

    public Chunk(String content, String sourceFile, int startLine, int endLine) {
        this.id = UUID.randomUUID().toString();
        this.content = content;
        this.sourceFile = sourceFile;
        this.startLine = startLine;
        this.endLine = endLine;
        this.metadata = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public double[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(double[] embedding) {
        this.embedding = embedding;
    }

    public void putMetadata(String key, String value) {
        this.metadata.put(key, value);
    }
}
