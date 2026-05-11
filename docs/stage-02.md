# Stage 02: LLM Provider 抽象层

## 做了什么

- 定义 `LlmClient` 接口，将 LLM 调用抽象为 Provider 无关的统一 API
- 实现 `ClaudeClient` 作为主要 Provider，用 `@ConditionalOnProperty` 控制激活
- 实现 `OpenAiClient` 作为备选 Provider（桩实现）
- 通过 `LlmClientConfiguration` 集中管理 Provider Bean 注册

新增/修改的关键类：
- `LlmClient` — Provider 无关接口，定义 `chat()` / `chatStream()` / `ask()`
- `ClaudeClient` — Claude 实现，`@ConditionalOnProperty(name="llm.provider", havingValue="claude")`
- `OpenAiClient` — OpenAI 实现
- `ModelConfig` — 模型配置 POJO（name, temperature, maxTokens）

## 解决了什么问题

**核心问题**：硬编码单个 LLM Provider 导致无法切换模型、难以测试、无法降级。

**学习目标**：
- Interface + Strategy 模式解耦 LLM 提供商
- Spring `@ConditionalOnProperty` 实现运行时 Bean 选择
- 理解不同模型的消息格式差异和统一适配

## 怎么做的

1. **接口抽象**：`LlmClient` 接口定义 `chat(List<ChatMessage>, ModelConfig) → ChatResponse`，提供 `ask(String)` 便捷方法
2. **条件注册**：`ClaudeClient` 使用 `@ConditionalOnProperty(name="llm.provider", havingValue="claude", matchIfMissing=true)`，默认激活
3. **统一配置**：`ModelConfig` 用 Builder 模式封装模型参数（name, maxTokens, temperature）
4. **默认方法**：`chatStream()` 和带 tools 的 `chat()` 重载用 `default` 方法提供兜底实现

## 关键文件

- `src/main/java/com/agent/llm/LlmClient.java` — Provider 无关接口
- `src/main/java/com/agent/llm/claude/ClaudeClient.java` — Claude 实现
- `src/main/java/com/agent/llm/openai/OpenAiClient.java` — OpenAI 实现
- `src/main/java/com/agent/llm/model/ModelConfig.java` — 模型配置 Builder
- `src/main/java/com/agent/llm/LlmClientConfiguration.java` — 配置类

## 验证方式

修改 `application.yml` 中 `llm.provider` 的值：
- `claude` → `ClaudeClient` 被注入
- `openai` → `OpenAiClient` 被注入
- 不设置 → 默认 `ClaudeClient`（`matchIfMissing=true`）
