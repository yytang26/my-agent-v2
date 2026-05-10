package com.agent.context;

import com.agent.memory.Message;
import com.agent.memory.MessageRole;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ToolResultPruner implements CompressionStrategy {

    private static final int TOOL_RESULT_MAX_LENGTH = 500;
    private static final int KEEP_HEAD = 200;
    private static final int KEEP_TAIL = 100;
    private static final int RECENT_ROUNDS_PROTECT = 3;

    public List<Message> prune(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> result = new ArrayList<>();
        int totalMessages = messages.size();

        for (int i = 0; i < totalMessages; i++) {
            Message msg = messages.get(i);
            boolean isRecent = i >= totalMessages - RECENT_ROUNDS_PROTECT;

            if (msg.role() == MessageRole.TOOL && !isRecent && msg.content().length() > TOOL_RESULT_MAX_LENGTH) {
                String pruned = msg.content().substring(0, KEEP_HEAD)
                        + "...(截断)..."
                        + msg.content().substring(msg.content().length() - KEEP_TAIL);
                result.add(new Message(MessageRole.TOOL, pruned, msg.timestamp()));
            } else {
                result.add(msg);
            }
        }

        return result;
    }

    @Override
    public List<Message> compress(List<Message> messages, int targetTokens) {
        return prune(messages);
    }

    @Override
    public String name() {
        return "ToolResultPruner";
    }
}
