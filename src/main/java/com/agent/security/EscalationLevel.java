package com.agent.security;

public enum EscalationLevel {
    INFO,    // 信息提示，不需要人工介入
    WARN,    // 警告提示，建议人工关注但不阻塞
    BLOCK    // 必须人工确认才能继续
}
