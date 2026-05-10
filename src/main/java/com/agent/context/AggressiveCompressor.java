package com.agent.context;

import com.agent.memory.Message;
import com.agent.memory.MessageRole;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AggressiveCompressor implements CompressionStrategy {

    private final TokenEstimator tokenEstimator;

    public AggressiveCompressor(TokenEstimator tokenEstimator) {
        this.tokenEstimator = tokenEstimator;
    }

    @Override
    public List<Message> compress(List<Message> messages, int targetTokens) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> result = new ArrayList<>();
        Message systemMsg = null;
        int dropCount = 0;

        for (Message msg : messages) {
            if (msg.role() == MessageRole.SYSTEM) {
                systemMsg = msg;
            }
        }

        if (systemMsg != null) {
            result.add(systemMsg);
        }

        List<Message> nonSystem = new ArrayList<>();
        for (Message msg : messages) {
            if (msg.role() != MessageRole.SYSTEM) {
                nonSystem.add(msg);
            }
        }

        int keepCount = Math.min(3, nonSystem.size());
        if (nonSystem.size() > keepCount) {
            dropCount = nonSystem.size() - keepCount;
            String summary = "[上下文已压缩: 之前讨论了 " + dropCount + " 轮对话]";
            result.add(Message.system(summary));
        }

        int startIndex = nonSystem.size() - keepCount;
        for (int i = startIndex; i < nonSystem.size(); i++) {
            result.add(nonSystem.get(i));
        }

        return result;
    }

    @Override
    public String name() {
        return "Aggressive";
    }
}
