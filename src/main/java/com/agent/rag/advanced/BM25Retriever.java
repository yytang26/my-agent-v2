package com.agent.rag.advanced;

import com.agent.rag.Chunk;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class BM25Retriever {

    private static final double K1 = 1.2;
    private static final double B = 0.75;

    // 倒排索引: 词 -> chunkId -> 频率
    private final Map<String, Map<String, Integer>> invertedIndex = new HashMap<>();
    // chunkId -> 文档长度（词数）
    private final Map<String, Integer> docLengths = new HashMap<>();
    // chunkId -> Chunk
    private final Map<String, Chunk> chunkMap = new HashMap<>();

    private double avgDocLength = 0.0;
    private int totalDocs = 0;

    public void index(List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            String chunkId = chunk.getId();
            chunkMap.put(chunkId, chunk);

            List<String> tokens = tokenize(chunk.getContent());
            docLengths.put(chunkId, tokens.size());

            Map<String, Integer> termFreq = new HashMap<>();
            for (String token : tokens) {
                termFreq.merge(token, 1, Integer::sum);
            }

            for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
                invertedIndex.computeIfAbsent(entry.getKey(), k -> new HashMap<>())
                        .put(chunkId, entry.getValue());
            }
        }

        totalDocs = docLengths.size();
        if (totalDocs > 0) {
            int totalLength = docLengths.values().stream().mapToInt(Integer::intValue).sum();
            avgDocLength = (double) totalLength / totalDocs;
        }
    }

    public List<ScoredChunk> search(String query, int topK) {
        if (invertedIndex.isEmpty() || query == null || query.isBlank()) {
            return List.of();
        }

        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return List.of();
        }

        Map<String, Double> scores = new HashMap<>();

        for (String token : queryTokens) {
            Map<String, Integer> postings = invertedIndex.get(token);
            if (postings == null || postings.isEmpty()) {
                continue;
            }

            int df = postings.size();
            double idf = Math.log(1.0 + (totalDocs - df + 0.5) / (df + 0.5));

            for (Map.Entry<String, Integer> entry : postings.entrySet()) {
                String chunkId = entry.getKey();
                int tf = entry.getValue();
                int docLen = docLengths.getOrDefault(chunkId, 0);
                double docLenFactor = 1.0 - B + B * (docLen / (avgDocLength + 1e-10));
                double bm25 = idf * ((tf * (K1 + 1.0)) / (tf + K1 * docLenFactor));
                scores.merge(chunkId, bm25, Double::sum);
            }
        }

        return scores.entrySet().stream()
                .map(e -> new ScoredChunk(chunkMap.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparingDouble(ScoredChunk::getScore).reversed())
                .limit(topK)
                .toList();
    }

    public void clear() {
        invertedIndex.clear();
        docLengths.clear();
        chunkMap.clear();
        avgDocLength = 0.0;
        totalDocs = 0;
    }

    public int size() {
        return chunkMap.size();
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }

        // 按空格/标点分割
        Pattern pattern = Pattern.compile("[\\s\\p{Punct}]+");
        String[] parts = pattern.split(text.toLowerCase());

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            // 如果是纯ASCII（英文单词），直接加入
            if (part.matches("[a-z0-9]+")) {
                tokens.add(part);
            } else {
                // 中文按单字分割
                for (char c : part.toCharArray()) {
                    if (Character.isLetterOrDigit(c)) {
                        tokens.add(String.valueOf(c).toLowerCase());
                    }
                }
            }
        }
        return tokens;
    }
}
