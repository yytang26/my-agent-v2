package com.agent.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

@Component
public class RagPipeline {

    private static final Logger log = LoggerFactory.getLogger(RagPipeline.class);

    private final DocumentChunker documentChunker;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;

    public RagPipeline(DocumentChunker documentChunker,
                       EmbeddingClient embeddingClient,
                       VectorStore vectorStore) {
        this.documentChunker = documentChunker;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
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

            vectorStore.addAll(chunks);
            log.info("Indexed file: {} -> {} chunks", filePath, chunks.size());
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
                    totalFiles++;
                    totalChunks += chunks.size();
                    log.info("Indexed: {} -> {} chunks", file, chunks.size());
                } catch (IOException e) {
                    log.warn("Failed to index file: {} - {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk directory: " + dirPath, e);
        }

        log.info("Directory indexed: {} files, {} chunks total", totalFiles, totalChunks);
    }

    public List<Chunk> retrieve(String query, int topK) {
        double[] queryVector = embeddingClient.embed(query);
        return vectorStore.search(queryVector, topK);
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
}
