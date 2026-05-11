# Stage 08: Agent Loop 架构（核心循环）

## 做了什么

- 实现核心 Agent 循环：query → LLM → (tool_call → execute → feed back) → repeat
- 状态追踪：THINKING → CALLING_TOOL → RESPONDING
- 最大迭代保护（防死循环）
- 支持多轮工具调用序列
- 组合 IntentRouter 按意图激活工具集

新增/修改的关键类：
- `AgentLoop` — 核心循环编排，`run(userMessage) → AgentResponse`
- `AgentState` — 状态枚举：THINKING, CALLING_TOOL, WAITING_RESULT, RESPONDING
- `AgentLoopConfig` — 循环配置（maxIterations）
- `TurnResult` — 单轮结果（state, text, toolCalls, toolResults, iteration）
- `AgentResponse` — 最终响应（finalMessage, turns, iterationCount, hitMaxIterations）

## 解决了什么问题

**核心问题**：有了工具但缺少编排——谁来决定何时调工具、何时回复用户、何时停止？

**学习目标**：
- 理解 Agent Loop 的核心模式：LLM 决策 → 工具执行 → 结果反馈 → LLM 再决策
- 掌握循环终止条件（max iterations, 无更多 tool calls）
- 学会处理多轮工具调用序列

## 怎么做的

1. **主循环**：`while (iteration < maxIterations)` 循环中调用 `llmClient.chat(messages, config, tools)`
2. **状态驱动**：LLM 返回 `tool_use` → 状态切为 `CALLING_TOOL`，执行工具后继续循环；返回纯文本 → 状态切为 `RESPONDING`，结束循环
3. **工具执行**：解析 `ContentBlock` 中的 `tool_use`，通过 `ToolExecutor.execute()` 执行，将 `tool_result` 加入 memory
4. **上下文管理**：每次循环前调用 `contextCompressor.compressIfNeeded()` + `preflightTokenCheck.willOverflow()` 防溢出
5. **溢出恢复**：捕获 `ContextOverflowException` → 调用 `contextOverflowHandler.handleOverflow()` 压缩后重试
6. **Intent 集成**：`intentRouter.route(userMessage)` 按意图筛选传给 LLM 的工具列表

## 关键文件

- `src/main/java/com/agent/core/AgentLoop.java` — 核心循环
- `src/main/java/com/agent/core/AgentState.java` — 状态枚举
- `src/main/java/com/agent/core/AgentLoopConfig.java` — 循环配置
- `src/main/java/com/agent/core/TurnResult.java` — 单轮结果
- `src/main/java/com/agent/core/AgentResponse.java` — 最终响应

## 验证方式

1. "现在几点" → Agent 调 `get_current_time` → 返回时间
2. 需要多步工具调用的场景 → 全部按序执行
3. 超过 maxIterations → 返回友好提示而非死循环
