package com.agent.core;

public enum AgentState {
    THINKING,        // 等待 LLM 响应
    CALLING_TOOL,    // 正在执行工具
    WAITING_RESULT,  // 等待工具结果
    RESPONDING       // 生成最终回复
}
