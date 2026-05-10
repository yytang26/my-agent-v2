package com.agent.context;

import com.agent.memory.Message;

import java.util.List;

public interface CompressionStrategy {

    List<Message> compress(List<Message> messages, int targetTokens);

    String name();
}
