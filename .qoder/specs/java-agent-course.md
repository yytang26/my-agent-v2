# Java Agent 学习课程 — 完整实施计划

## Context

目标用户是一位 Java 开发工程师，希望从零开始构建一个类似 Claude Code / Qoder 的 AI Agent。现有项目只有一个 `roadmap.md`，没有任何代码。通过对 Claude Code 源码的深入分析，发现现有 roadmap 缺少以下关键功能：CLI 交互界面、权限系统、文件操作工具、代码搜索工具、会话持久化、流式工具执行、配置管理、错误恢复机制、成本追踪。本课程将 roadmap 重构为 26 个循序渐进的阶段，最终产出一个可用的 CLI + Web GUI Agent。

---

## 技术栈

- **语言**: Java 17+
- **构建工具**: Maven
- **框架**: Spring Boot 3.x
- **CLI 库**: JLine 3（终端交互）
- **LLM 主要对接**: Claude API (Anthropic)
- **序列化**: Jackson JSON
- **HTTP 客户端**: Spring WebClient (for streaming SSE)
- **代码组织**: 每阶段一个 git branch（`stage-01`, `stage-02`, ...）
- **远程仓库**: `git@github.com:yytang26/my-agent-v2.git`

---

## 课程结构总览（26 阶段，6 个 Phase）

| Phase | 阶段 | 主题 | 核心产出 |
|-------|------|------|---------|
| 1. 基础设施 | 01-05 | LLM 通信基础 | 能调用 Claude API 并处理错误/成本 |
| 2. Agent 核心 | 06-10 | Agent 大脑 | 有记忆、能调工具的 Agent Loop |
| 3. CLI 界面 | 11-14 | 终端交互 | 可用的命令行 Agent（类似简化版 CC） |
| 4. 智能增强 | 15-18 | 上下文与推理 | 更聪明的 Agent（压缩、恢复、路由） |
| 5. 知识与扩展 | 19-22 | RAG、MCP、子Agent | 可扩展的知识体系 |
| 6. Web 与生产 | 23-26 | Web GUI + 生产化 | 完整可部署的产品 |

---

## Phase 1: 基础设施（Stages 01-05）

### Stage 01: 项目脚手架 & 裸调 Claude API
**Branch:** `stage-01`

**学习目标:**
- 搭建 Maven + Spring Boot 项目
- 理解 Claude Messages API 的请求/响应格式
- 实现最简单的 `ask(prompt) → response`

**关键交付物:**
- Spring Boot 启动项目（`pom.xml` 含 spring-boot-starter-web, jackson）
- `ClaudeRawClient` 类：用 `RestTemplate` POST 到 `/v1/messages`
- 一个 main 方法：硬编码 prompt，打印 LLM 回复到控制台
- `application.yml` 配置 API key（gitignore）

**核心类:**
- `com.agent.Application` — Spring Boot 入口
- `com.agent.llm.ClaudeRawClient` — 原始 HTTP 调用
- `com.agent.llm.model.ChatMessage` — 消息 POJO
- `com.agent.llm.model.ChatResponse` — 响应 POJO

**验证:** `mvn spring-boot:run` 无报错，控制台打印 Claude 的回复

---

### Stage 02: LLM Provider 抽象层
**Branch:** `stage-02`

**学习目标:**
- Interface + Strategy 模式解耦 LLM 提供商
- 实现运行时切换模型的能力
- 理解不同模型的消息格式差异

**关键交付物:**
- `LlmClient` 接口：`chat(List<ChatMessage>, ModelConfig) → ChatResponse`
- `ClaudeClient` 实现（完整）
- `OpenAiClient` 桩实现（结构完整，逻辑简单）
- `LlmClientFactory` 根据配置解析活跃的 provider

**核心类:**
- `com.agent.llm.LlmClient` — Provider 无关接口
- `com.agent.llm.claude.ClaudeClient` — Claude 实现
- `com.agent.llm.openai.OpenAiClient` — OpenAI 实现
- `com.agent.llm.model.ModelConfig` — 模型配置（name, temperature, maxTokens）
- `com.agent.llm.LlmClientFactory` — 工厂/注册表

**验证:** 切换 `application.yml` 中的 provider 配置 → 正确的 Client 被注入

---

### Stage 03: 分层配置管理
**Branch:** `stage-03`

**学习目标:**
- 实现类似 git config 的层级配置系统
- 全局 → 用户 → 项目 → CLI → Session 五层优先级
- 类型安全的配置访问

**关键交付物:**
- `ConfigManager` 加载并合并多路径配置
  - `~/.my-agent/config.yml`（全局）
  - `./.my-agent/config.yml`（项目级）
  - CLI 参数（启动时解析）
  - Session 覆盖（运行时修改）
- 类型化访问 + 校验

**核心类:**
- `com.agent.config.ConfigManager` — 合并分层配置
- `com.agent.config.ConfigSource` — 枚举：GLOBAL, USER, PROJECT, CLI, SESSION
- `com.agent.config.AgentConfig` — 类型化配置 POJO
- `com.agent.config.ConfigLoader` — 文件 I/O + YAML 解析

**验证:** 项目配置覆盖全局配置；CLI 参数覆盖文件配置

---

### Stage 04: 错误处理、重试与限流
**Branch:** `stage-04`

**学习目标:**
- 实现带 jitter 的指数退避重试
- 处理 Claude 特有错误（429/529/500）
- Provider 降级链（主 Provider 挂了 → 切备用）

**关键交付物:**
- `RetryPolicy` + `RetryExecutor`（指数退避 + jitter）
- `RateLimiter`（滑动窗口令牌桶）
- `FallbackChain`（有序 Provider 列表）
- 类型化异常体系（`RateLimitException`, `OverloadedException`）

**核心类:**
- `com.agent.resilience.RetryPolicy` — 重试配置
- `com.agent.resilience.RetryExecutor` — 重试执行器
- `com.agent.resilience.RateLimiter` — 限流器
- `com.agent.resilience.FallbackChain` — 降级链
- `com.agent.llm.exception.LlmException` — 异常层级

**验证:** 模拟 429 → 观察退避重试日志；模拟持续失败 → 切换到备用 Provider

---

### Stage 05: 成本追踪与 Token 统计
**Branch:** `stage-05`

**学习目标:**
- 从每次 LLM 响应中解析 token 使用量
- 维护会话级和累积成本计数器
- 实现定价表（模型 → 单价）

**关键交付物:**
- `TokenTracker` 累积每次调用的 input/output tokens
- `CostCalculator` 根据模型定价计算费用
- Hook 到 `LlmClient` 响应自动追踪
- 摘要方法："本次会话：15,234 tokens, $0.08"

**核心类:**
- `com.agent.tracking.TokenTracker` — Token 累积器
- `com.agent.tracking.CostCalculator` — 成本计算
- `com.agent.tracking.UsageSummary` — DTO
- `com.agent.tracking.PricingRegistry` — 定价表

**验证:** 3 次 LLM 调用后查询 tracker → 显示正确累积 token 数和费用

---

## Phase 2: Agent 核心（Stages 06-10）

### Stage 06: 对话记忆
**Branch:** `stage-06`

**学习目标:**
- 实现内存中的消息历史（最简"记忆"）
- 理解上下文无限增长的代价
- 为后续压缩打下基础

**关键交付物:**
- `ConversationMemory` 接口 + `InMemoryConversationMemory` 实现
- 系统提示词注入到 position 0
- 当前上下文的 token 估算

**核心类:**
- `com.agent.memory.ConversationMemory` — 接口
- `com.agent.memory.InMemoryConversationMemory` — ArrayList 实现
- `com.agent.memory.Message` — 不可变消息记录
- `com.agent.memory.MessageRole` — 枚举：SYSTEM, USER, ASSISTANT, TOOL

**验证:** 发送 5 条消息 → LLM 能感知前文语境

---

### Stage 07: Tool 框架（注解驱动注册）
**Branch:** `stage-07`

**学习目标:**
- 用 Java 注解实现可插拔的工具注册
- 从注解自动生成 JSON Schema
- 理解 Claude 的 `tool_use` / `tool_result` 消息格式

**关键交付物:**
- `@Tool` 注解 + `@ToolParam` 注解
- `ToolRegistry` 扫描 Spring Bean 注册所有 @Tool 方法
- JSON Schema 生成器
- `ToolExecutor` 按名称调用工具
- Demo 工具：`get_current_time()`

**核心类:**
- `com.agent.tool.Tool`（注解）
- `com.agent.tool.ToolParam`（注解）
- `com.agent.tool.ToolRegistry` — 工具注册表
- `com.agent.tool.ToolDefinition` — Schema 表示
- `com.agent.tool.ToolExecutor` — 执行器
- `com.agent.tool.ToolResult` — 结果包装

**验证:** 启动时发现 @Tool 注解的方法 → 发送需要工具调用的 prompt → 工具执行 → 结果回传 LLM

---

### Stage 08: Agent Loop 架构（核心循环）
**Branch:** `stage-08`

**学习目标:**
- 实现核心 Agent 循环：query → LLM → (tool call → execute → feed back) → repeat
- 处理多轮工具调用序列
- 循环终止条件（max iterations, 无更多 tool calls）

**关键交付物:**
- `AgentLoop` 主编排类
- 状态追踪：THINKING → CALLING_TOOL → WAITING_RESULT → RESPONDING
- 最大迭代保护（防死循环）
- 单轮内并行工具执行

**核心类:**
- `com.agent.core.AgentLoop` — 核心循环
- `com.agent.core.AgentState` — 状态枚举
- `com.agent.core.AgentLoopConfig` — 循环配置
- `com.agent.core.TurnResult` — 单轮结果
- `com.agent.core.AgentResponse` — 最终响应

**验证:** "现在几点" → 调 get_current_time → 返回时间；需要 3 次工具调用的场景 → 全部按序执行

---

### Stage 09: 文件系统工具
**Branch:** `stage-09`

**学习目标:**
- 实现编码 Agent 的核心文件操作工具
- 路径安全（防目录遍历）
- 大文件和二进制文件处理

**关键交付物:**
- `ReadFileTool` — 读文件（支持行范围）
- `WriteFileTool` — 写/创建文件
- `EditFileTool` — 行级文本替换（old_text → new_text）
- `ListDirectoryTool` — 目录列表
- `PathValidator` — 所有操作限制在工作区根目录

**核心类:**
- `com.agent.tool.fs.ReadFileTool`
- `com.agent.tool.fs.WriteFileTool`
- `com.agent.tool.fs.EditFileTool`
- `com.agent.tool.fs.ListDirectoryTool`
- `com.agent.tool.fs.PathValidator`

**验证:** "读 pom.xml" → 返回内容；"创建 hello.txt" → 文件生成；试读 `/etc/passwd` → PathValidator 阻止

---

### Stage 10: 代码工具（Grep, Glob, Bash）
**Branch:** `stage-10`

**学习目标:**
- 编码助手必备的搜索工具
- 安全的 Bash 执行（超时、输出截断）
- Java ProcessBuilder 进程管理

**关键交付物:**
- `GrepTool` — 正则搜索文件内容
- `GlobTool` — 文件模式匹配（`**/*.java`）
- `BashTool` — Shell 命令执行（超时 + 输出截断）
- `ProcessRunner` — ProcessBuilder 封装
- `OutputTruncator` — 大输出截断

**核心类:**
- `com.agent.tool.code.GrepTool`
- `com.agent.tool.code.GlobTool`
- `com.agent.tool.code.BashTool`
- `com.agent.tool.code.ProcessRunner`
- `com.agent.tool.code.OutputTruncator`

**验证:** "找所有含 TODO 的 Java 文件" → GrepTool 返回结果；`mvn --version` → BashTool 返回版本号

---

## Phase 3: CLI 界面（Stages 11-14）

### Stage 11: CLI REPL（交互式终端）
**Branch:** `stage-11`

**学习目标:**
- 用 JLine 3 构建交互式 REPL
- 命令历史、自动补全、多行输入
- 终端 Markdown 渲染（颜色、代码块）
- Ctrl+C 优雅处理

**关键交付物:**
- JLine 终端 Reader + 历史
- Markdown → ANSI 颜色渲染器
- 命令前缀：`/help`, `/clear`, `/exit`, `/cost`
- 等待 LLM 时的 Spinner 动画
- 多行输入支持

**核心类:**
- `com.agent.cli.CliRepl` — 主 REPL 循环
- `com.agent.cli.TerminalRenderer` — Markdown → ANSI
- `com.agent.cli.CommandHandler` — 斜杠命令调度
- `com.agent.cli.InputReader` — JLine 封装
- `com.agent.cli.Spinner` — 等待动画

**验证:** 启动 → 交互提示符；输入消息 → 彩色 Markdown 回复；`/cost` → 显示费用

---

### Stage 12: 权限系统
**Branch:** `stage-12`

**学习目标:**
- 实现分层权限模型（自动允许/提示/拒绝）
- 为危险操作设置交互确认
- Session 级 "always allow" 机制

**关键交付物:**
- `PermissionManager` 规则：ALLOW, ASK, DENY per tool
- 默认策略：读 → ALLOW, 写 → ASK, bash → ASK
- 交互确认："Agent 想写入 src/Main.java — 允许? [y/n/always]"
- "always" 为 session 级白名单

**核心类:**
- `com.agent.permission.PermissionManager`
- `com.agent.permission.PermissionRule`
- `com.agent.permission.PermissionPolicy`（枚举）
- `com.agent.permission.InteractivePrompter`
- `com.agent.permission.SessionAllowList`

**验证:** 写文件 → 弹出确认；回答 "always" → 后续写操作不再询问

---

### Stage 13: 会话持久化
**Branch:** `stage-13`

**学习目标:**
- 对话状态 JSON 序列化到磁盘
- 按 ID 恢复历史会话
- 自动保存（防崩溃丢失）

**关键交付物:**
- `SessionManager` 管理 `~/.my-agent/sessions/` 下的 JSON 文件
- 每个 session：唯一 ID, 消息列表, 元数据
- CLI 命令：`/save`, `/resume <id>`, `/sessions`, `/new`
- 每轮自动保存

**核心类:**
- `com.agent.session.SessionManager`
- `com.agent.session.Session`
- `com.agent.session.SessionSerializer`
- `com.agent.session.SessionMetadata`

**验证:** 聊 5 轮 → 退出 → 重启 → `/resume` → 上下文保留

---

### Stage 14: 流式输出
**Branch:** `stage-14`

**学习目标:**
- Claude API SSE 流式响应
- Token 逐字渲染到终端
- 流中检测 tool_use 事件
- 流式 + 工具执行管线

**关键交付物:**
- `StreamingClaudeClient` 基于 SSE 的流式客户端
- 终端增量输出（打字机效果）
- 流中 tool_use 检测 → 暂停 → 执行工具 → 恢复
- 网络中断 → 优雅降级

**核心类:**
- `com.agent.llm.claude.StreamingClaudeClient`
- `com.agent.llm.model.StreamChunk`
- `com.agent.core.StreamingAgentLoop`
- `com.agent.cli.StreamRenderer`
- `com.agent.llm.ChunkAccumulator`

**验证:** 提问 → token 逐字出现（非一次性）；需要工具时 → 流暂停 → 执行 → 恢复

---

## Phase 4: 智能增强（Stages 15-18）

### Stage 15: 上下文压缩与裁剪
**Branch:** `stage-15`

**学习目标:**
- 多策略压缩（滑动窗口、摘要、工具结果裁剪）
- 分层压缩管线（按阈值触发不同策略）
- Token 估算启发式

**核心类:** `ContextCompressor`, `SlidingWindowCompressor`, `SummaryCompressor`, `ToolResultPruner`, `TieredCompression`, `TokenEstimator`

**验证:** 100 条消息 → 压缩触发 → token 数下降；摘要后仍能回忆关键信息

---

### Stage 16: 上下文溢出恢复
**Branch:** `stage-16`

**学习目标:**
- 处理 413 / context_length_exceeded 错误
- 自动压缩 → 重试恢复管线
- 预飞检查（发送前估算 token）

**核心类:** `ContextOverflowHandler`, `PreflightTokenCheck`, `AggressiveCompressor`, `RecoveryStrategy`

**验证:** 人为超限 → 413 → 自动恢复 → 重试成功

---

### Stage 17: ReAct 循环（结构化推理）
**Branch:** `stage-17`

**学习目标:**
- Thought → Action → Observation 推理模式
- 对比原生 function calling vs 纯文本 ReAct
- 格式违反时的容错

**核心类:** `ReActAgent`, `ReActParser`, `ReActPromptBuilder`, `LoopDetector`

**验证:** 复杂查询 → 显示 Thought 步骤；格式违反 → 优雅恢复

---

### Stage 18: 意图识别与路由
**Branch:** `stage-18`

**学习目标:**
- 轻量分类步骤（省钱避免无谓操作）
- 按意图激活不同工具集
- 用更便宜的模型做分类

**核心类:** `IntentClassifier`, `IntentRouter`, `Intent`（枚举）, `RoutingConfig`

**验证:** "查天气" → GENERAL；"找所有 TODO" → CODE → 激活代码工具

---

## Phase 5: 知识与扩展（Stages 19-22）

### Stage 19: 基础 RAG
**Branch:** `stage-19`

**学习目标:**
- 文档分块策略
- Embedding + 向量相似搜索
- 检索结果注入 prompt

**核心类:** `DocumentChunker`, `EmbeddingClient`, `VectorStore`, `RagPipeline`, `Chunk`

**验证:** 索引 10 个文件 → 问相关问题 → 检索到正确 chunk

---

### Stage 20: 高级 RAG（多路召回、RRF、Rerank）
**Branch:** `stage-20`

**学习目标:**
- BM25 + 向量双路召回
- RRF 融合
- LLM Reranking
- HyDE 查询改写

**核心类:** `BM25Retriever`, `RRFMerger`, `LlmReranker`, `QueryRewriter`, `AdvancedRagPipeline`

**验证:** 对比基础 RAG vs 高级 RAG → 相关性提升

---

### Stage 21: MCP Client + 插件系统
**Branch:** `stage-21`

**学习目标:**
- 实现 MCP 协议客户端
- 连接外部 MCP Server 动态发现工具
- 插件加载机制

**核心类:** `McpClient`, `McpTransport`, `McpToolAdapter`, `PluginLoader`, `McpServerConfig`

**验证:** 配置 MCP Server → 启动时发现并注册工具 → Agent 透明使用

---

### Stage 22: SubAgent + Prompt 模板系统
**Branch:** `stage-22`

**学习目标:**
- 子 Agent 委派（独立上下文）
- Skill（LLM 触发）/ Command（用户触发）模板系统
- 模板变量插值

**核心类:** `SubAgent`, `AgentOrchestrator`, `PromptTemplate`, `SkillRegistry`, `CommandRegistry`, `TemplateRenderer`

**验证:** 复杂任务 → 委派子 Agent → 返回结果；`/review` 命令 → 加载模板

---

## Phase 6: Web 与生产（Stages 23-26）

### Stage 23: Web GUI（SSE + 聊天界面）
**Branch:** `stage-23`

**学习目标:**
- Spring Boot REST + SSE 端点
- HTML/CSS/JS 聊天 UI（支持 Markdown、代码高亮）
- 多用户并发会话
- 模拟网页聊天对话（课程要求）

**关键交付物:**
- `POST /api/chat` (SSE streaming)
- `GET /api/sessions`, `POST /api/sessions`
- 聊天 UI：消息气泡、Markdown 渲染、代码高亮
- 工具执行进度展示
- 斜杠命令面板

**核心类:**
- `com.agent.web.ChatController`
- `com.agent.web.WebSessionManager`
- `src/main/resources/static/` — 前端文件

**验证:** 浏览器打开 → 聊天 UI → 输入消息 → 流式响应

---

### Stage 24: 高级任务规划
**Branch:** `stage-24`

**学习目标:**
- 任务分解为 DAG
- 子任务状态机
- 并行子任务执行
- 失败后重规划

**核心类:** `TaskPlanner`, `TaskDAG`, `TaskExecutor`, `TaskState`, `Replanner`

**验证:** 复杂任务 → 分解为 4 个子任务 → 独立的并行执行 → 失败时重规划

---

### Stage 25: 安全、沙箱与 Human-in-the-Loop
**Branch:** `stage-25`

**学习目标:**
- Prompt 注入检测
- 沙箱执行（资源限制）
- 风险评分
- 置信度驱动的人工介入

**核心类:** `InjectionDetector`, `SandboxExecutor`, `RiskScorer`, `HumanEscalation`, `OutputSanitizer`

**验证:** 注入模式 → 检测拦截；`rm -rf /` → 沙箱阻止

---

### Stage 26: 生产工程化 & 毕业项目
**Branch:** `stage-26`

**学习目标:**
- 数据库持久化（H2/SQLite）
- 长期记忆（跨会话实体提取）
- 可观测性（日志、指标）
- 评估框架
- 打包为可分发 JAR

**关键交付物:**
- Spring Data JPA 持久化 session
- 结构化日志 + 关联 ID
- 20 个测试用例的评估套件
- `mvn package` → 可执行 JAR
- 完整 README

**核心类:** `SessionRepository`, `LongTermMemory`, `MetricsCollector`, `AgentEvaluator`, `EvalTestCase`

**验证:** 杀进程重启 → session 不丢；评估套件 → 报告成功率；`java -jar agent.jar` → 完整启动

---

## 与原 Roadmap 的对比（补充内容）

| 原 Roadmap 缺失的功能 | 本课程对应阶段 |
|----------------------|--------------|
| CLI 终端界面（REPL）| Stage 11 |
| 文件系统工具 | Stage 09 |
| 代码搜索工具（Grep/Glob/Bash）| Stage 10 |
| 权限系统 | Stage 12 |
| 分层配置管理 | Stage 03 |
| 会话持久化 | Stage 13 |
| 错误恢复与重试 | Stage 04 |
| 成本追踪 | Stage 05 |
| 上下文溢出恢复 | Stage 16 |
| 流式输出（终端级）| Stage 14 |

---

## 每个阶段的工作流

```
1. 从上一阶段的 branch 创建新 branch: git checkout -b stage-XX
2. 实现该阶段的功能代码
3. 确保 `mvn compile` 无错误
4. 确保 `mvn spring-boot:run` 可启动并验证功能
5. git add + commit
6. git push origin stage-XX
7. 下一阶段从此 branch 继续
```

---

## 首次 Push 准备

1. `git init`（如未初始化）
2. `git remote add origin git@github.com:yytang26/my-agent-v2.git`
3. 配置 SSH key 免密（使用已有 key 或生成新 key）
4. 首次 push 后续操作免密

---

## 验证策略

- 每阶段完成后：`mvn compile` → `mvn spring-boot:run` → 功能演示
- Phase 3 完成后（Stage 14）：完整 CLI 交互演示
- Phase 6 完成后（Stage 23）：浏览器打开 Web UI 演示
- Stage 26：运行评估套件 + 打包验证

---

## 关键文件路径

- `pom.xml` — Maven 项目定义（Stage 01 创建）
- `src/main/java/com/agent/core/AgentLoop.java` — 核心循环（Stage 08）
- `src/main/java/com/agent/tool/ToolRegistry.java` — 工具注册（Stage 07）
- `src/main/java/com/agent/cli/CliRepl.java` — CLI 界面（Stage 11）
- `src/main/java/com/agent/web/ChatController.java` — Web 端点（Stage 23）
- `src/main/resources/static/index.html` — 聊天 UI（Stage 23）
- `src/main/resources/application.yml` — 配置文件
