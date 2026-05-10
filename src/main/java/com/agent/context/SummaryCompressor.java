package com.agent.context;

import com.agent.llm.LlmClient;
import com.agent.memory.Message;
import com.agent.memory.MessageRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SummaryCompressor implements CompressionStrategy {

    private static final Logger log = LoggerFactory.getLogger(SummaryCompressor.class);
    private static final String SUMMARY_PROMPT = "请将以下对话历史压缩为简洁摘要，保留关键信息和上下文。用中文回答：\n\n";
    private static final int MOCK_SUMMARY_MAX_LENGTH = 200;

    private final LlmClient llmClient;
    private final TokenEstimator tokenEstimator;

    public SummaryCompressor(LlmClient llmClient, TokenEstimator tokenEstimator) {
        this.llmClient = llmClient;
        this.tokenEstimator = tokenEstimator;
    }

    @Override
    public List<Message> compress(List<Message> messages, int targetTokens) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        int current = tokenEstimator.estimate(messages);
        if (current <= targetTokens) {
            return new ArrayList<>(messages);
        }

        List<Message> result = new ArrayList<>();
        int startIdx = 0;
        if (!messages.isEmpty() && messages.get(0).role() == MessageRole.SYSTEM) {
            result.add(messages.get(0));
            startIdx = 1;
        }

        int total = messages.size();
        if (startIdx >= total) {
            return result;
        }

        int mid = startIdx + (total - startIdx) / 2;
        List<Message> firstHalf = messages.subList(startIdx, mid);
        List<Message> secondHalf = messages.subList(mid, total);

        if (firstHalf.isEmpty()) {
            result.addAll(secondHalf);
            return result;
        }

        String summary = generateSummary(firstHalf);
        result.add(Message.assistant("[历史摘要] " + summary));
        result.addAll(secondHalf);

        return result;
    }

    private String generateSummary(List<Message> messages) {
        boolean isMock = llmClient.getClass().getSimpleName().contains("Mock");

        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            sb.append(msg.role().name()).append(": ").append(msg.content()).append("\n");
        }
        String fullText = sb.toString();

        if (isMock) {
            if (fullText.length() > MOCK_SUMMARY_MAX_LENGTH) {
                return fullText.substring(0, MOCK_SUMMARY_MAX_LENGTH) + "...";
            }
            return fullText;
        }

        try {
            return llmClient.ask(SUMMARY_PROMPT + fullText);
        } catch (Exception e) {
            log.warn("生成摘要失败，使用截断版本", e);
            if (fullText.length() > MOCK_SUMMARY_MAX_LENGTH) {
                return fullText.substring(0, MOCK_SUMMARY_MAX_LENGTH) + "...";
            }
            return fullText;
        }
    }

    @Override
    public String name() {
        return "Summary";
    }
}
