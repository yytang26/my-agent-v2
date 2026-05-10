package com.agent.tool.builtin;

import com.agent.rag.Chunk;
import com.agent.rag.RagPipeline;
import com.agent.rag.advanced.AdvancedRagPipeline;
import com.agent.tool.Tool;
import com.agent.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class RagTool {

    private final RagPipeline ragPipeline;
    private final AdvancedRagPipeline advancedRagPipeline;

    private volatile String mode = "advanced";

    public RagTool(RagPipeline ragPipeline, AdvancedRagPipeline advancedRagPipeline) {
        this.ragPipeline = ragPipeline;
        this.advancedRagPipeline = advancedRagPipeline;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    @Tool(name = "index_files", description = "索引文件到知识库")
    public String indexFiles(
            @ToolParam(name = "path", description = "文件或目录路径") String path,
            @ToolParam(name = "pattern", description = "文件匹配模式（如 *.java），仅在索引目录时有效", required = false) String pattern) {
        try {
            Path filePath = Path.of(path).toAbsolutePath().normalize();
            if (!Files.exists(filePath)) {
                return "Error: 路径不存在: " + path;
            }

            if (Files.isDirectory(filePath)) {
                if ("basic".equalsIgnoreCase(mode)) {
                    ragPipeline.indexDirectory(path, pattern);
                } else {
                    advancedRagPipeline.indexDirectory(path, pattern);
                }
                return "已索引目录: " + path + (pattern != null ? " (模式: " + pattern + ")" : "");
            } else {
                if ("basic".equalsIgnoreCase(mode)) {
                    ragPipeline.indexFile(path);
                } else {
                    advancedRagPipeline.indexFile(path);
                }
                return "已索引文件: " + path;
            }
        } catch (Exception e) {
            return "索引失败: " + e.getMessage();
        }
    }

    @Tool(name = "search_knowledge", description = "从知识库中搜索相关内容")
    public String searchKnowledge(
            @ToolParam(name = "query", description = "搜索查询") String query,
            @ToolParam(name = "top_k", description = "返回结果数，默认 5", required = false) Integer topK,
            @ToolParam(name = "mode", description = "检索模式: basic 或 advanced，默认 advanced", required = false) String searchMode) {
        int k = topK != null ? topK : 5;
        String effectiveMode = searchMode != null ? searchMode.toLowerCase() : mode;
        List<Chunk> chunks;
        if ("basic".equalsIgnoreCase(effectiveMode)) {
            chunks = ragPipeline.retrieve(query, k);
        } else {
            chunks = advancedRagPipeline.retrieve(query, k);
        }

        if (chunks.isEmpty()) {
            return "未找到相关内容。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("搜索结果 (共 ").append(chunks.size()).append(" 条, 模式: ").append(effectiveMode).append(")\n\n");
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            sb.append(i + 1).append(". ");
            sb.append("[").append(chunk.getSourceFile());
            sb.append(" 行").append(chunk.getStartLine());
            sb.append("-").append(chunk.getEndLine()).append("]\n");
            sb.append(chunk.getContent()).append("\n\n");
        }
        return sb.toString().trim();
    }
}
