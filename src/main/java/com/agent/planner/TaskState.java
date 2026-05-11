package com.agent.planner;

public enum TaskState {
    PENDING,      // 等待执行
    IN_PROGRESS,  // 执行中
    COMPLETED,    // 已完成
    FAILED,       // 失败
    CANCELLED,    // 已取消
    BLOCKED       // 被依赖阻塞
}
