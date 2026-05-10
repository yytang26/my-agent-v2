package com.agent.rag.advanced;

import com.agent.rag.Chunk;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RRFMerger {

    private static final int K = 60;

    public List<ScoredChunk> merge(List<List<ScoredChunk>> rankedLists, int topK) {
        if (rankedLists == null || rankedLists.isEmpty()) {
            return List.of();
        }

        // 去重：按 chunkId 聚合 RRF 分数
        Map<String, RrfItem> itemMap = new HashMap<>();

        for (List<ScoredChunk> list : rankedLists) {
            if (list == null || list.isEmpty()) {
                continue;
            }
            for (int rank = 0; rank < list.size(); rank++) {
                ScoredChunk sc = list.get(rank);
                if (sc == null || sc.getChunk() == null) {
                    continue;
                }
                String chunkId = sc.getChunkId();
                RrfItem item = itemMap.computeIfAbsent(chunkId,
                        id -> new RrfItem(sc.getChunk(), 0.0));
                // RRF 公式: score(d) = Σ 1/(k + rank_i(d))
                item.score += 1.0 / (K + rank + 1);
            }
        }

        return itemMap.values().stream()
                .map(item -> new ScoredChunk(item.chunk, item.score))
                .sorted(Comparator.comparingDouble(ScoredChunk::getScore).reversed())
                .limit(topK)
                .toList();
    }

    private static class RrfItem {
        final Chunk chunk;
        double score;

        RrfItem(Chunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
