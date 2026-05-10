package com.agent.rag.advanced;

import com.agent.rag.Chunk;
import com.agent.rag.DocumentChunker;
import com.agent.rag.EmbeddingClient;
import com.agent.rag.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

@Component
public class AdvancedRagPipeline {

    private static final Logger log = LoggerFactory.getLogger(AdvancedRagPipeline.class);

    private final VectorStore vectorStore;
    private final BM25Retriever bm25Retriever;
    private final EmbeddingClient embeddingClient;
    private final RRFMerger rrfMerger;
    private final LlmReranker llmReranker;
    private final QueryRewriter queryRewriter;
    private final DocumentChunker documentChunker;

    private final boolean useHyde;
    private final boolean useReranker;
    private final double bm25Weight;
    private final double vectorWeight;

    public AdvancedRagPipeline(VectorStore vectorStore,
                               BM25Retriever bm25Retriever,
                               EmbeddingClient embeddingClient,
                               RRFMerger rrfMerger,
                               LlmReranker llmReranker,
                               QueryRewriter queryRewriter,
                               DocumentChunker documentChunker,
                               @Value("${agent.rag.advanced.use-hyde:true}") boolean useHyde,
                               @Value("${agent.rag.advanced.use-reranker:false}") boolean useReranker,
                               @Value("${agent.rag.advanced.bm25-weight:1.0}") double bm25Weight,
                               @Value("${agent.rag.advanced.vector-weight:1.0}") double vectorWeight) {
        this.vectorStore = vectorStore;
        this.bm25Retriever = bm25Retriever;
        this.embeddingClient = embeddingClient;
        this.rrfMerger = rrfMerger;
        this.llmReranker = llmReranker;
        this.queryRewriter = queryRewriter;
        this.documentChunker = documentChunker;
        this.useHyde = useHyde;
        this.useReranker = useReranker;
        this.bm25Weight = bm25Weight;
        this.vectorWeight = vectorWeight;
    }

    public List<Chunk> retrieve(String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String searchQuery = query;
        String hydeQuery = null;

        // 1. 可选：QueryRewriter 改写查询 (HyDE)
        if (useHyde) {
            hydeQuery = queryRewriter.rewrite(query);
            log.info("[AdvancedRag] HyDE 改写查询: {}", hydeQuery);
        }

        // 2. 多路召回
        List<List<ScoredChunk>> rankedLists = new ArrayList<>();

        // 路1: VectorStore 向量搜索（原始 query）
        double[] queryVector = embeddingClient.embed(searchQuery);
        List<Chunk> vectorResults = vectorStore.search(queryVector, topK * 2);
        List<ScoredChunk> vectorScored = scoreChunks(vectorResults, vectorWeight);
        if (!vectorScored.isEmpty()) {
            rankedLists.add(vectorScored);
            log.info("[AdvancedRag] 向量召回 {} 条", vectorScored.size());
        }

        // 路2: BM25Retriever 关键词搜索
        List<ScoredChunk> bm25Results = bm25Retriever.search(searchQuery, topK * 2);
        List<ScoredChunk> bm25Scored = scaleScores(bm25Results, bm25Weight);
        if (!bm25Scored.isEmpty()) {
            rankedLists.add(bm25Scored);
            log.info("[AdvancedRag] BM25 召回 {} 条", bm25Scored.size());
        }

        // 路3: VectorStore 向量搜索（HyDE 改写后的文本 embedding）
        if (useHyde && hydeQuery != null && !hydeQuery.equals(query)) {
            double[] hydeVector = embeddingClient.embed(hydeQuery);
            List<Chunk> hydeResults = vectorStore.search(hydeVector, topK * 2);
            List<ScoredChunk> hydeScored = scoreChunks(hydeResults, vectorWeight);
            if (!hydeScored.isEmpty()) {
                rankedLists.add(hydeScored);
                log.info("[AdvancedRag] HyDE 向量召回 {} 条", hydeScored.size());
            }
        }

        if (rankedLists.isEmpty()) {
            return List.of();
        }

        // 3. RRFMerger 融合多路结果
        List<ScoredChunk> merged = rrfMerger.merge(rankedLists, topK * 2);
        log.info("[AdvancedRag] RRF 融合后 {} 条", merged.size());

        // 4. 可选：LlmReranker 重排序
        if (useReranker) {
            merged = llmReranker.rerank(query, merged, topK);
            log.info("[AdvancedRag] LLM 重排序后 {} 条", merged.size());
        }

        // 5. 返回 Top-K 结果
        List<Chunk> result = merged.stream()
                .limit(topK)
                .map(ScoredChunk::getChunk)
                .toList();
        log.info("[AdvancedRag] 最终返回 {} 条结果", result.size());
        return result;
    }

    public void indexFile(String filePath) {
        Path path = Path.of(filePath).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Path is not a file: " + filePath);
        }

        try {
            String content = Files.readString(path);
            List<Chunk> chunks = documentChunker.chunk(content, filePath);

            for (Chunk chunk : chunks) {
                double[] embedding = embeddingClient.embed(chunk.getContent());
                chunk.setEmbedding(embedding);
            }

            // 同时建立向量索引和 BM25 索引
            vectorStore.addAll(chunks);
            bm25Retriever.index(chunks);

            log.info("[AdvancedRag] Indexed file: {} -> {} chunks", filePath, chunks.size());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    public void indexDirectory(String dirPath, String pattern) {
        Path dir = Path.of(dirPath).toAbsolutePath().normalize();
        if (!Files.exists(dir)) {
            throw new IllegalArgumentException("Directory not found: " + dirPath);
        }
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Path is not a directory: " + dirPath);
        }

        String globPattern = pattern != null && !pattern.isBlank() ? pattern : "*";
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);

        int totalFiles = 0;
        int totalChunks = 0;

        try (var stream = Files.walk(dir)) {
            List<Path> files = stream.filter(Files::isRegularFile)
                    .filter(p -> matcher.matches(p.getFileName()))
                    .toList();

            for (Path file : files) {
                try {
                    String content = Files.readString(file);
                    List<Chunk> chunks = documentChunker.chunk(content, file.toString());

                    for (Chunk chunk : chunks) {
                        double[] embedding = embeddingClient.embed(chunk.getContent());
                        chunk.setEmbedding(embedding);
                    }

                    vectorStore.addAll(chunks);
                    bm25Retriever.index(chunks);

                    totalFiles++;
                    totalChunks += chunks.size();
                    log.info("[AdvancedRag] Indexed: {} -> {} chunks", file, chunks.size());
                } catch (IOException e) {
                    log.warn("[AdvancedRag] Failed to index file: {} - {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk directory: " + dirPath, e);
        }

        log.info("[AdvancedRag] Directory indexed: {} files, {} chunks total", totalFiles, totalChunks);
    }

    public String buildAugmentedPrompt(String query, List<Chunk> relevantChunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是相关的上下文信息：\n");
        for (Chunk chunk : relevantChunks) {
            sb.append("---\n");
            sb.append(String.format("[来源: %s, 行 %d-%d]%n",
                    chunk.getSourceFile(), chunk.getStartLine(), chunk.getEndLine()));
            sb.append(chunk.getContent()).append("\n");
            sb.append("---\n");
        }
        sb.append("\n基于以上上下文，请回答用户问题：\n");
        sb.append(query);
        return sb.toString();
    }

    private List<ScoredChunk> scoreChunks(List<Chunk> chunks, double weight) {
        List<ScoredChunk> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            double score = (chunks.size() - i) * weight; // 排名分数，越靠前越高
            result.add(new ScoredChunk(chunks.get(i), score));
        }
        return result;
    }

    private List<ScoredChunk> scaleScores(List<ScoredChunk> chunks, double weight) {
        List<ScoredChunk> result = new ArrayList<>();
        for (ScoredChunk sc : chunks) {
            result.add(new ScoredChunk(sc.getChunk(), sc.getScore() * weight));
        }
        return result;
    }
}
