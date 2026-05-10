package com.agent.rag.advanced;

import com.agent.llm.LlmClient;
import com.agent.rag.Chunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class LlmReranker {

    private static final Logger log = LoggerFactory.getLogger(LlmReranker.class);

    private final LlmClient llmClient;
    private final boolean mockMode;

    public LlmReranker(LlmClient llmClient,
                       @Value("${llm.provider:mock}") String llmProvider) {
        this.llmClient = llmClient;
        this.mockMode = "mock".equalsIgnoreCase(llmProvider);
    }

    public List<ScoredChunk> rerank(String query, List<ScoredChunk> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        if (mockMode) {
            log.info("[LlmReranker] Mock 模式，直接返回原排序");
            return candidates.stream().limit(topK).toList();
        }

        String prompt = buildRerankPrompt(query, candidates);
        String response;
        try {
            response = llmClient.ask(prompt);
        } catch (Exception e) {
            log.warn("[LlmReranker] LLM 重排序失败，回退到原排序: {}", e.getMessage());
            return candidates.stream().limit(topK).toList();
        }

        List<Integer> order = parseRerankResponse(response, candidates.size());
        log.info("[LlmReranker] LLM 重排序结果: {}", order);

        Map<String, ScoredChunk> candidateMap = candidates.stream()
                .collect(Collectors.toMap(ScoredChunk::getChunkId, sc -> sc, (a, b) -> a));

        List<ScoredChunk> result = new ArrayList<>();
        for (int idx : order) {
            if (idx >= 0 && idx < candidates.size()) {
                ScoredChunk sc = candidates.get(idx);
                if (!result.contains(sc)) {
                    result.add(new ScoredChunk(sc.getChunk(), sc.getScore() + 1.0));
                }
            }
        }

        // 补充未出现在排序中的候选
        for (ScoredChunk sc : candidates) {
            if (!result.contains(sc)) {
                result.add(new ScoredChunk(sc.getChunk(), sc.getScore()));
            }
        }

        return result.stream().limit(topK).toList();
    }

    private String buildRerankPrompt(String query, List<ScoredChunk> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("对以下文档按与查询的相关性从高到低排序。\n\n");
        sb.append("查询: ").append(query).append("\n\n");
        sb.append("文档:\n");
        for (int i = 0; i < candidates.size(); i++) {
            Chunk chunk = candidates.get(i).getChunk();
            sb.append(i + 1).append(". ")
                    .append(chunk.getContent().replace("\n", " "))
                    .append("\n");
        }
        sb.append("\n请输出排序后的文档编号，格式如：2, 1, 3, 4");
        return sb.toString();
    }

    private List<Integer> parseRerankResponse(String response, int maxIndex) {
        List<Integer> result = new ArrayList<>();
        if (response == null || response.isBlank()) {
            return result;
        }

        // 提取数字
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(response);
        while (matcher.find()) {
            int num = Integer.parseInt(matcher.group()) - 1; // 1-based -> 0-based
            if (num >= 0 && num < maxIndex && !result.contains(num)) {
                result.add(num);
            }
        }
        return result;
    }
}
