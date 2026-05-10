package com.agent.llm.model;

public enum StreamEventType {
    MESSAGE_START,
    CONTENT_BLOCK_START,
    CONTENT_BLOCK_DELTA,
    CONTENT_BLOCK_STOP,
    MESSAGE_DELTA,
    MESSAGE_STOP
}
