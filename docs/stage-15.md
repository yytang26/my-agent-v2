# Stage 15: 上下文压缩与裁剪

## 做了什么

- 多策略压缩（滑动窗口、摘要、工具结果裁剪）
- 分层压缩管线（`TieredCompression` 按阈值触发不同策略）
- Token 估算启发式（`TokenEstimator`）
- `CompressionStrategy` 接口统一压缩策略

新增/修改的关键类：
- `ContextCompressor` — 入口，组合 `TokenEstimator` + `TieredCompression`
- `CompressionStrategy` — 接口：`compress(messages, targetTokens) → List<Message>`
- `SlidingWindowCompressor` — 保留最近 N 条消息，丢弃早期对话
- `SummaryCompressor` — 将早期消息压缩为摘要
- `ToolResultPruner` — 裁剪过长的工具返回结果
- `TieredCompression` — 分层决策：Level 0 不压缩 / Level 1 轻度 / Level 2 激进
- `TokenEstimator` — 启发式 token 估算（chars / 4）

## 解决了什么问题

**核心问题**：对话越长 token 越多，超出上下文窗口后 LLM 无法处理。

**学习目标**：
- 多策略压缩的设计思路
- 分层触发的阈值管理
- Token 估算的启发式方法

## 怎么做的

1. **Token 估算**：`TokenEstimator.estimate(messages)` 按字符数 / 4 近似估算 token 数
2. **分层决策**：`TieredCompression.decide(currentTokens)` → 根据占窗口比例返回 `CompressionPlan`（level, strategies, targetTokens）
3. **策略管线**：`ContextCompressor.compressIfNeeded()` 按计划依次执行策略，每步都有日志记录压缩前后 token 数
4. **滑动窗口**：`SlidingWindowCompressor` 保留 system prompt + 最近 N 条消息
5. **工具结果裁剪**：`ToolResultPruner` 对超过阈值的 `tool_result` 截断，保留前后部分 + `[truncated]`
6. **摘要压缩**：`SummaryCompressor` 将早期消息合为一条摘要消息

## 关键文件

- `src/main/java/com/agent/context/ContextCompressor.java` — 压缩入口
- `src/main/java/com/agent/context/CompressionStrategy.java` — 策略接口
- `src/main/java/com/agent/context/SlidingWindowCompressor.java` — 滑动窗口
- `src/main/java/com/agent/context/SummaryCompressor.java` — 摘要压缩
- `src/main/java/com/agent/context/ToolResultPruner.java` — 工具结果裁剪
- `src/main/java/com/agent/context/TieredCompression.java` — 分层决策
- `src/main/java/com/agent/context/TokenEstimator.java` — Token 估算

## 验证方式

1. 发送 100 条消息 → 压缩触发 → token 数下降
2. 检查日志中的压缩策略名称和前后 token 数
3. 压缩后仍能回忆关键信息（不丢失 system prompt 和最近对话）
