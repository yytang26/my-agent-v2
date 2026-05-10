package com.agent.context;

import com.agent.memory.Message;
import com.agent.memory.MessageRole;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SlidingWindowCompressor implements CompressionStrategy {

    private final TokenEstimator tokenEstimator;

    public SlidingWindowCompressor(TokenEstimator tokenEstimator) {
        this.tokenEstimator = tokenEstimator;
    }

    @Override
    public List<Message> compress(List<Message> messages, int targetTokens) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> mutable = new ArrayList<>(messages);
        int current = tokenEstimator.estimate(mutable);
        if (current <= targetTokens) {
            return mutable;
        }

        int dropIndex = 0;
        if (!mutable.isEmpty() && mutable.get(0).role() == MessageRole.SYSTEM) {
            dropIndex = 1;
        }

        while (dropIndex < mutable.size() && tokenEstimator.estimate(mutable) > targetTokens) {
            mutable.remove(dropIndex);
        }

        return mutable;
    }

    @Override
    public String name() {
        return "SlidingWindow";
    }
}
