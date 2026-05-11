# Stage 05: 成本追踪与 Token 统计

## 做了什么

- 从每次 LLM 响应中自动解析 token 使用量（input/output tokens）
- 维护会话级和累积成本计数器
- 实现定价表（模型 → 单价映射）
- Hook 到 `ResilientLlmClient` 响应自动追踪
- 提供费用摘要：`"本次会话：15,234 tokens, $0.08"`

新增/修改的关键类：
- `TokenTracker` — Token 累积器，记录每次调用的 input/output tokens
- `CostCalculator` — 根据模型定价计算费用
- `UsageSummary` — DTO，包含总 token 数、总费用
- `PricingRegistry` — 定价表，维护各模型的 input/output 单价

## 解决了什么问题

**核心问题**：LLM 调用按 token 计费，不追踪成本会导致预算失控。

**学习目标**：
- 理解 LLM API 响应中的 `usage` 字段（`input_tokens`, `output_tokens`）
- 学会构建定价表和成本计算器
- 掌握 Hook 模式：在不修改核心逻辑的前提下插入追踪逻辑

## 怎么做的

1. **自动追踪**：`ResilientLlmClient.chat()` 每次成功调用后，检查 `response.getUsage()`，调用 `tokenTracker.recordUsage(model, inputTokens, outputTokens)`
2. **Token 累积**：`TokenTracker` 用 `AtomicLong` 线程安全地累积 input/output token 数
3. **成本计算**：`CostCalculator` 从 `PricingRegistry` 查询单价，计算 `inputTokens * inputPrice + outputTokens * outputPrice`
4. **费用摘要**：`getSummary()` 返回 `UsageSummary`，包含总 token、总费用、调用次数
5. **定价表**：`PricingRegistry` 硬编码主流模型单价（claude-3-opus: $15/$75 per MTok, sonnet: $3/$15, haiku: $0.25/$1.25）

## 关键文件

- `src/main/java/com/agent/tracking/TokenTracker.java` — Token 累积器
- `src/main/java/com/agent/tracking/CostCalculator.java` — 成本计算
- `src/main/java/com/agent/tracking/UsageSummary.java` — 费用摘要 DTO
- `src/main/java/com/agent/tracking/PricingRegistry.java` — 模型定价表

## 验证方式

1. 执行 3 次 LLM 调用
2. 调用 `tokenTracker.getSummary()` → 显示正确的累积 token 数和费用
3. CLI 中输入 `/cost` → 显示会话费用摘要
