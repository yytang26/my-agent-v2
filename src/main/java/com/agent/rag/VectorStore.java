package com.agent.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class VectorStore {

    private final List<Chunk> chunks = new ArrayList<>();

    public synchronized void add(Chunk chunk) {
        chunks.add(chunk);
    }

    public synchronized void addAll(List<Chunk> chunks) {
        this.chunks.addAll(chunks);
    }

    public synchronized List<Chunk> search(double[] queryVector, int topK) {
        if (queryVector == null) {
            return List.of();
        }
        return chunks.stream()
                .filter(c -> c.getEmbedding() != null)
                .sorted(Comparator.comparingDouble(c -> -cosineSimilarity(c.getEmbedding(), queryVector)))
                .limit(topK)
                .toList();
    }

    public synchronized int size() {
        return chunks.size();
    }

    public synchronized void clear() {
        chunks.clear();
    }

    public static double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
