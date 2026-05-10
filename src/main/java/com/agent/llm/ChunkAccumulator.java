package com.agent.llm;

import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ContentBlock;
import com.agent.llm.model.StreamChunk;
import com.agent.llm.model.StreamEventType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChunkAccumulator {

    private final List<ContentBlock> contentBlocks = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String currentBlockType;
    private StringBuilder currentText = new StringBuilder();
    private String currentToolUseId;
    private String currentToolName;
    private StringBuilder currentToolInputJson = new StringBuilder();
    private boolean hasToolUse = false;

    public void accumulate(StreamChunk chunk) {
        switch (chunk.getType()) {
            case CONTENT_BLOCK_START -> {
                if (chunk.getToolUseId() != null) {
                    currentBlockType = "tool_use";
                    currentToolUseId = chunk.getToolUseId();
                    currentToolName = chunk.getToolName();
                    currentToolInputJson = new StringBuilder();
                    hasToolUse = true;
                } else if (chunk.getText() != null) {
                    currentBlockType = "text";
                    currentText = new StringBuilder();
                    currentText.append(chunk.getText());
                }
            }
            case CONTENT_BLOCK_DELTA -> {
                if ("tool_use".equals(currentBlockType) && chunk.getText() != null) {
                    currentToolInputJson.append(chunk.getText());
                } else if (chunk.getText() != null) {
                    if (currentBlockType == null) {
                        currentBlockType = "text";
                        currentText = new StringBuilder();
                    }
                    currentText.append(chunk.getText());
                }
            }
            case CONTENT_BLOCK_STOP -> {
                if ("tool_use".equals(currentBlockType)) {
                    Map<String, Object> input = parseToolInput(currentToolInputJson.toString());
                    contentBlocks.add(ContentBlock.toolUse(currentToolUseId, currentToolName, input));
                    currentBlockType = null;
                } else if ("text".equals(currentBlockType)) {
                    contentBlocks.add(ContentBlock.text(currentText.toString()));
                    currentBlockType = null;
                }
            }
            case MESSAGE_START, MESSAGE_DELTA, MESSAGE_STOP -> {
                // 暂不处理 message 级别的元数据
            }
        }
    }

    public ChatResponse toResponse() {
        ChatResponse response = new ChatResponse();
        // 如果有正在累积的文本块但未收到 STOP，先 flush
        flushCurrentBlock();
        response.setContent(new ArrayList<>(contentBlocks));
        return response;
    }

    public boolean hasToolUse() {
        flushCurrentBlock();
        for (ContentBlock block : contentBlocks) {
            if ("tool_use".equals(block.getType())) {
                return true;
            }
        }
        return hasToolUse;
    }

    public String getCurrentText() {
        return currentText.toString();
    }

    public List<ContentBlock> getToolUseBlocks() {
        flushCurrentBlock();
        List<ContentBlock> result = new ArrayList<>();
        for (ContentBlock block : contentBlocks) {
            if ("tool_use".equals(block.getType())) {
                result.add(block);
            }
        }
        return result;
    }

    private void flushCurrentBlock() {
        if ("text".equals(currentBlockType) && currentText.length() > 0) {
            contentBlocks.add(ContentBlock.text(currentText.toString()));
            currentBlockType = null;
            currentText = new StringBuilder();
        } else if ("tool_use".equals(currentBlockType) && currentToolUseId != null) {
            Map<String, Object> input = parseToolInput(currentToolInputJson.toString());
            contentBlocks.add(ContentBlock.toolUse(currentToolUseId, currentToolName, input));
            currentBlockType = null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseToolInput(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
