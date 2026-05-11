# Stage 16: 上下文溢出恢复

## 做了什么

- 处理 413 / `context_length_exceeded` 错误的自动恢复管线
- 预飞检查（发送前估算 token，避免无效请求）
- `AggressiveCompressor` 激进压缩策略
- `RecoveryStrategy` 恢复策略接口

新增/修改的关键类：
- `ContextOverflowHandler` — 溢出处理器，组合 `RecoveryStrategy` + `PreflightTokenCheck`
- `PreflightTokenCheck` — 预飞 token 检查，`willOverflow(messages, contextWindowSize)`
- `AggressiveCompressor` — 激进压缩：大幅裁剪历史，只保留关键上下文
- `RecoveryStrategy` — 恢复策略接口：`recover(messages, maxTokens) → List<Message>`

## 解决了什么问题

**核心问题**：即使有常规压缩，仍可能出现上下文超限（413 错误），Agent 直接崩溃。

**学习目标**：
- 溢出错误的自动恢复模式
- 预飞检查避免浪费 API 调用
- 激进压缩与温和压缩的取舍

## 怎么做的

1. **预飞检查**：`PreflightTokenCheck.willOverflow()` 在发送前估算 token 数，若超限则提前压缩
2. **AgentLoop 集成**：`AgentLoop.run()` 中每轮先 `compressIfNeeded()` → 再 `preflightTokenCheck.willOverflow()` → 若仍超限则 `contextOverflowHandler.ensureFit()`
3. **异常捕获**：`catch (ContextOverflowException)` → 调用 `contextOverflowHandler.handleOverflow()` 恢复 → 用压缩后的消息重试
4. **重试限制**：最多重试 2 次溢出恢复，避免无限循环
5. **激进压缩**：`AggressiveCompressor` 在常规压缩不够时进一步裁剪，大幅缩减历史消息
6. **RecoveryStrategy**：统一恢复接口，组合 `SlidingWindowCompressor` + `ToolResultPruner` + `AggressiveCompressor`

## 关键文件

- `src/main/java/com/agent/context/ContextOverflowHandler.java` — 溢出处理器
- `src/main/java/com/agent/context/PreflightTokenCheck.java` — 预飞检查
- `src/main/java/com/agent/context/AggressiveCompressor.java` — 激进压缩
- `src/main/java/com/agent/context/RecoveryStrategy.java` — 恢复策略

## 验证方式

1. 人为构造超长对话 → 触发 413 → 自动恢复 → 重试成功
2. 日志显示 "预检超限, 启动上下文恢复" 和压缩前后 token 数
3. 恢复后的对话仍能正常继续
