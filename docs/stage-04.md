# Stage 04: 错误处理、重试与限流

## 做了什么

- 实现带 jitter 的指数退避重试机制（`RetryPolicy` + `RetryExecutor`）
- 实现滑动窗口限流器（`RateLimiter`）
- 实现 Provider 降级链（`FallbackChain`）
- 构建类型化异常体系，区分可重试/不可重试错误
- 组合为 `ResilientLlmClient` 装饰器，统一包装重试+限流+降级+Token追踪

新增/修改的关键类：
- `RetryPolicy` — 重试配置（maxRetries, initialDelayMs, backoffMultiplier, jitterFactor）
- `RetryExecutor` — 指数退避 + jitter 重试执行器
- `RateLimiter` — 滑动窗口令牌桶限流器
- `FallbackChain` — 有序 Provider 列表，主 Provider 失败自动切换备用
- `ResilientLlmClient` — 组合装饰器，`@Primary` Bean
- `LlmException` / `RateLimitException` / `OverloadedException` / `ContextOverflowException` / `AuthenticationException` — 异常层级

## 解决了什么问题

**核心问题**：LLM API 调用不稳定——429 限流、529 过载、网络超时等，直接失败导致用户体验差。

**学习目标**：
- 指数退避 + jitter 防止惊群效应
- 令牌桶限流保护 API 配额
- Fallback Chain 实现 Provider 降级
- 异常分类驱动不同恢复策略

## 怎么做的

1. **重试机制**：`RetryExecutor.executeWithRetry()` 包裹 Supplier，仅对 `isRetryable()=true` 的异常重试，延迟按 `initialDelay * backoff^attempt + jitter` 计算
2. **限流器**：`RateLimiter` 用 `ReentrantLock` + `Condition` 实现滑动窗口，`acquire()` 阻塞等待直到有配额
3. **降级链**：`FallbackChain` 持有 `List<LlmClient>`，依次尝试，不可重试异常直接跳到下一个 Provider
4. **装饰器**：`ResilientLlmClient` 实现 `LlmClient` 接口，内部组合 rateLimiter → retryExecutor → fallbackChain 的调用管线
5. **异常体系**：`LlmException` 为基类，子类标记 `retryable` 属性，429 → `RateLimitException`，529 → `OverloadedException`

## 关键文件

- `src/main/java/com/agent/resilience/RetryPolicy.java` — 重试配置 Builder
- `src/main/java/com/agent/resilience/RetryExecutor.java` — 重试执行器
- `src/main/java/com/agent/resilience/RateLimiter.java` — 滑动窗口限流器
- `src/main/java/com/agent/resilience/FallbackChain.java` — Provider 降级链
- `src/main/java/com/agent/resilience/ResilientLlmClient.java` — 组合装饰器
- `src/main/java/com/agent/llm/exception/` — 异常层级

## 验证方式

1. 模拟 429 响应 → 观察退避重试日志（延迟递增）
2. 设置 `resilience.retry.enabled=false` → 不重试直接抛异常
3. 配置多个 Provider → 主 Provider 失败后自动切换到备用
