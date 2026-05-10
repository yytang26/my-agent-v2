package com.agent.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentChunker {

    private final int maxChunkSize;
    private final int overlap;

    public DocumentChunker(
            @Value("${rag.chunk-size:500}") int maxChunkSize,
            @Value("${rag.overlap:50}") int overlap) {
        this.maxChunkSize = Math.max(maxChunkSize, overlap * 2 + 1);
        this.overlap = overlap;
    }

    public List<Chunk> chunk(String content, String sourceFile) {
        List<Chunk> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return chunks;
        }

        // 优先按段落分割（双换行）
        String[] rawParagraphs = content.split("\n\n");
        int lineOffset = 1;

        for (String rawParagraph : rawParagraphs) {
            String paragraph = rawParagraph.trim();
            if (paragraph.isEmpty()) {
                lineOffset += countLines(rawParagraph) + 1;
                continue;
            }

            int paraStartLine = lineOffset;
            int paraLines = countLines(paragraph);

            if (paragraph.length() <= maxChunkSize) {
                chunks.add(new Chunk(paragraph, sourceFile, paraStartLine, paraStartLine + paraLines - 1));
            } else {
                chunks.addAll(chunkFixedSize(paragraph, sourceFile, paraStartLine));
            }

            lineOffset += countLines(rawParagraph) + 1;
        }

        return chunks;
    }

    private List<Chunk> chunkFixedSize(String text, String sourceFile, int startLine) {
        List<Chunk> chunks = new ArrayList<>();
        int pos = 0;

        while (pos < text.length()) {
            int end = Math.min(pos + maxChunkSize, text.length());
            // 尝试在单词边界切分
            if (end < text.length()) {
                int breakPoint = findBreakPoint(text, end);
                if (breakPoint > pos) {
                    end = breakPoint;
                }
            }

            String chunkText = text.substring(pos, end).trim();
            int linesBefore = countNewlines(text.substring(0, pos));
            int chunkLines = countLines(chunkText);
            int chunkStartLine = startLine + linesBefore;
            int chunkEndLine = chunkStartLine + Math.max(0, chunkLines - 1);

            chunks.add(new Chunk(chunkText, sourceFile, chunkStartLine, chunkEndLine));

            if (end == text.length()) {
                break;
            }
            pos = end - overlap;
            if (pos <= 0 || pos >= text.length()) {
                break;
            }
        }

        return chunks;
    }

    private int findBreakPoint(String text, int targetPos) {
        // 向前查找最近的空白字符或换行符
        for (int i = targetPos; i > targetPos - 20 && i > 0; i--) {
            char c = text.charAt(i);
            if (c == ' ' || c == '\n' || c == '\t') {
                return i;
            }
        }
        return targetPos;
    }

    private int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int count = 1;
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                count++;
            }
        }
        return count;
    }

    private int countNewlines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                count++;
            }
        }
        return count;
    }
}
