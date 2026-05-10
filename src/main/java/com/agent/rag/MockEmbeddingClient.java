package com.agent.rag;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("mock")
public class MockEmbeddingClient implements EmbeddingClient {

    private static final int DIMENSION = 128;

    @Override
    public double[] embed(String text) {
        if (text == null || text.isEmpty()) {
            double[] empty = new double[DIMENSION];
            // L2 归一化: 零向量保持为零
            return empty;
        }

        String normalized = text.toLowerCase();
        double[] vector = new double[DIMENSION];

        // 基于字符 bigram 频率
        for (int i = 0; i < normalized.length() - 1; i++) {
            String bigram = normalized.substring(i, i + 2);
            int idx = Math.abs(bigram.hashCode()) % DIMENSION;
            vector[idx] += 1.0;
        }

        // 额外特征维度
        vector[0] = normalized.length();
        vector[1] = countDistinctBigrams(normalized);

        // L2 归一化
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < DIMENSION; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    @Override
    public List<double[]> embedBatch(List<String> texts) {
        return texts.stream()
                .map(this::embed)
                .toList();
    }

    private int countDistinctBigrams(String text) {
        if (text.length() < 2) {
            return 0;
        }
        java.util.Set<String> set = new java.util.HashSet<>();
        for (int i = 0; i < text.length() - 1; i++) {
            set.add(text.substring(i, i + 2));
        }
        return set.size();
    }
}
