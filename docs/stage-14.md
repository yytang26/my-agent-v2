# Stage 14: 流式输出

## 做了什么

- 基于 Claude API SSE 的流式响应客户端
- Token 逐字渲染到终端（打字机效果）
- 流中检测 `tool_use` 事件 → 暂停流 → 执行工具 → 恢复
- `StreamingAgentLoop` 流式 + 工具执行管线
- `ChunkAccumulator` 流式 chunk 累积器

新增/修改的关键类：
- `StreamingClaudeClient` — SSE 流式客户端，`chatStream()` 返回 `Flux<StreamChunk>`
- `StreamChunk` — 流式事件 DTO（type, textDelta, toolName, toolUseId, inputDelta）
- `StreamingAgentLoop` — 流式 Agent 循环，实时渲染 + 工具执行
- `StreamRenderer` — 终端增量输出渲染器
- `ChunkAccumulator` — 流式 chunk 累积，最终组装为完整 `ChatResponse`

## 解决了什么问题

**核心问题**：非流式调用需要等 LLM 生成完毕才显示，长回复时用户体验差。

**学习目标**：
- Claude API SSE 流式响应的处理
- Reactor `Flux` 响应式编程
- 流中 `tool_use` 事件的检测与处理
- 流式累积器的状态管理

## 怎么做的

1. **SSE 解析**：`StreamingClaudeClient` 用 WebClient 接收 SSE 流，解析 `event:` + `data:` 行
2. **事件分类**：`StreamChunk` 区分 `CONTENT_BLOCK_DELTA`（文本增量）、`CONTENT_BLOCK_START`（tool_use 开始）、`MESSAGE_DELTA`（usage 统计）
3. **实时渲染**：`StreamingAgentLoop` 中 `stream.doOnNext(chunk -> streamRenderer.renderChunk(chunk))` 逐字输出
4. **ChunkAccumulator**：累积所有流式 chunk，`toResponse()` 组装完整 `ChatResponse`，`hasToolUse()` 检测是否有工具调用
5. **工具处理**：流结束后检查 `accumulator.hasToolUse()` → 执行工具 → 将结果加入 memory → 继续循环
6. **溢出恢复**：`onErrorResume` 捕获 `ContextOverflowException` → 压缩上下文 → 重试

## 关键文件

- `src/main/java/com/agent/llm/claude/StreamingClaudeClient.java` — SSE 流式客户端
- `src/main/java/com/agent/llm/model/StreamChunk.java` — 流式事件 DTO
- `src/main/java/com/agent/core/StreamingAgentLoop.java` — 流式 Agent 循环
- `src/main/java/com/agent/cli/StreamRenderer.java` — 终端增量渲染
- `src/main/java/com/agent/llm/ChunkAccumulator.java` — 流式累积器

## 验证方式

1. 提问 → token 逐字出现（非一次性刷新）
2. 需要工具时 → 流暂停 → 执行工具 → 继续流式输出
3. `/mode function` 使用流式模式 vs 非流式模式对比
