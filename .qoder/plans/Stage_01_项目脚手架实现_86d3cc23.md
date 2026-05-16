# Java Agent 全阶段实施计划 (Stage 01-26)

## 总览
基于 java-agent-course.md 规划，使用 Java 21 + Maven + Spring Boot 3.x，从零构建一个类似 Claude Code 的 AI Agent。支持 Mock 模式，无需真实 API key 即可开发测试。

---

## Stage 01: 项目脚手架 & 裸调 Claude API

**Branch:** `stage-01`

### Task 1: 创建 Maven + Spring Boot 项目结构
- `pom.xml`: Java 21, Spring Boot 3.x parent, spring-boot-starter-web, jackson-databind
- 目录: `src/main/java/com/agent/`, `src/main/resources/`
- `application.yml`: Claude API 配置 (key 通过环境变量注入)
- `.gitignore`: 排除 target/, IDE 文件, 敏感配置

### Task 2: 实现核心类
- `com.agent.Application` — Spring Boot 入口 + CommandLineRunner 演示
- `com.agent.llm.model.ChatMessage` — 消息 POJO (role, content)
- `com.agent.llm.model.ChatResponse` — 响应 POJO (id, content, model, usage)
- `com.agent.llm.ClaudeRawClient` — RestTemplate 调用 `/v1/messages`
- `com.agent.llm.MockClaudeClient` — Mock 实现，Spring Profile `mock` 激活

### Task 3: 验证
- `mvn compile` 通过
- `mvn spring-boot:run -Dspring-boot.run.profiles=mock` Mock 模式运行正常

---

## Stage 02: LLM Provider 抽象层

**Branch:** `stage-02`

### Task 1: Provider 接口与工厂
- `com.agent.llm.LlmClient` 接口: `chat(List<ChatMessage>, ModelConfig) -> ChatResponse`
- `com.agent.llm.LlmClientFactory` 根据配置创建对应 Client
- `com.agent.llm.model.ModelConfig` — 模型配置 POJO (name, temperature, maxTokens)

### Task 2: Provider 实现
- `com.agent.llm.claude.ClaudeClient` — 完整 Claude 实现 (从 Stage 01 重构)
- `com.agent.llm.openai.OpenAiClient` — OpenAI 桩实现
- `com.agent.llm.mock.MockLlmClient` — Mock 统一实现

### Task 3: 验证
- 切换 `application.yml` provider 配置 -> 正确 Client 注入
- Mock 模式下 Factory 返回 MockLlmClient

---

## Stage 03: 分层配置管理

**Branch:** `stage-03`

### Task 1: 配置层级体系
- `com.agent.config.ConfigSource` 枚举: GLOBAL, USER, PROJECT, CLI, SESSION
- `com.agent.config.ConfigLoader` — YAML 文件加载
- `com.agent.config.ConfigManager` — 多层配置合并 (优先级: SESSION > CLI > PROJECT > USER > GLOBAL)

### Task 2: 类型化配置
- `com.agent.config.AgentConfig` — 类型安全配置 POJO
- 配置路径: `~/.my-agent/config.yml` (全局), `./.my-agent/config.yml` (项目级)

### Task 3: 验证
- 项目配置覆盖全局配置; CLI 参数覆盖文件配置

---

## Stage 04: 错误处理、重试与限流

**Branch:** `stage-04`

### Task 1: 重试机制
- `com.agent.resilience.RetryPolicy` — 重试配置 (最大次数、退避参数)
- `com.agent.resilience.RetryExecutor` — 指数退避 + jitter 执行器

### Task 2: 限流与降级
- `com.agent.resilience.RateLimiter` — 滑动窗口令牌桶
- `com.agent.resilience.FallbackChain` — 有序 Provider 降级链

### Task 3: 异常体系
- `com.agent.llm.exception.LlmException` — 基类
- `RateLimitException`, `OverloadedException`, `AuthenticationException` 等

### Task 4: 验证
- 模拟 429 -> 退避重试; 持续失败 -> 切换备用 Provider

---

## Stage 05: 成本追踪与 Token 统计

**Branch:** `stage-05`

### Task 1: Token 追踪
- `com.agent.tracking.TokenTracker` — 累积 input/output tokens
- `com.agent.tracking.CostCalculator` — 根据模型定价计算费用
- `com.agent.tracking.PricingRegistry` — 模型定价表

### Task 2: 集成
- Hook 到 LlmClient 响应自动追踪
- `com.agent.tracking.UsageSummary` DTO: "本次会话: X tokens, $Y"

### Task 3: 验证
- 3 次 LLM 调用后 tracker 显示正确累积数据

---

## Stage 06: 对话记忆

**Branch:** `stage-06`

### Task 1: 记忆接口与实现
- `com.agent.memory.ConversationMemory` 接口
- `com.agent.memory.InMemoryConversationMemory` — ArrayList 实现
- `com.agent.memory.Message` — 不可变消息记录
- `com.agent.memory.MessageRole` 枚举: SYSTEM, USER, ASSISTANT, TOOL

### Task 2: 系统提示与 Token 估算
- 系统提示词注入 position 0
- 当前上下文 token 估算方法

### Task 3: 验证
- 发送 5 条消息 -> LLM 感知前文语境

---

## Stage 07: Tool 框架（注解驱动注册）

**Branch:** `stage-07`

### Task 1: 注解定义
- `com.agent.tool.Tool` 注解 (name, description)
- `com.agent.tool.ToolParam` 注解 (name, description, required)

### Task 2: 注册与执行
- `com.agent.tool.ToolRegistry` — 扫描 Spring Bean 注册所有 @Tool 方法
- `com.agent.tool.ToolDefinition` — JSON Schema 表示
- `com.agent.tool.ToolExecutor` — 按名称调用工具
- `com.agent.tool.ToolResult` — 结果包装

### Task 3: Schema 生成
- 从 @Tool + @ToolParam 注解自动生成 Claude tool_use 格式的 JSON Schema

### Task 4: Demo 工具
- `get_current_time()` 工具验证整个链路

### Task 5: 验证
- 启动发现 @Tool -> prompt 触发 tool_use -> 执行 -> 结果回传 LLM

---

## Stage 08: Agent Loop 架构（核心循环）

**Branch:** `stage-08`

### Task 1: 核心循环
- `com.agent.core.AgentLoop` — query -> LLM -> (tool call -> execute -> feed back) -> repeat
- `com.agent.core.AgentState` 枚举: THINKING, CALLING_TOOL, WAITING_RESULT, RESPONDING
- `com.agent.core.AgentLoopConfig` — 最大迭代数等配置

### Task 2: 循环控制
- 最大迭代保护（防死循环）
- 终止条件: 无更多 tool calls 或达到 max iterations
- 单轮内并行工具执行

### Task 3: 结果封装
- `com.agent.core.TurnResult` — 单轮结果
- `com.agent.core.AgentResponse` — 最终响应

### Task 4: 验证
- "现在几点" -> 调 get_current_time -> 返回时间
- 需要多次工具调用的场景 -> 全部按序执行

---

## Stage 09: 文件系统工具

**Branch:** `stage-09`

### Task 1: 路径安全
- `com.agent.tool.fs.PathValidator` — 所有操作限制在工作区根目录

### Task 2: 文件工具实现
- `ReadFileTool` — 读文件 (支持行范围)
- `WriteFileTool` — 写/创建文件
- `EditFileTool` — 行级文本替换 (old_text -> new_text)
- `ListDirectoryTool` — 目录列表

### Task 3: 验证
- "读 pom.xml" -> 返回内容
- "创建 hello.txt" -> 文件生成
- 试读 `/etc/passwd` -> PathValidator 阻止

---

## Stage 10: 代码工具（Grep, Glob, Bash）

**Branch:** `stage-10`

### Task 1: 搜索工具
- `com.agent.tool.code.GrepTool` — 正则搜索文件内容
- `com.agent.tool.code.GlobTool` — 文件模式匹配

### Task 2: Shell 执行
- `com.agent.tool.code.BashTool` — 超时 + 输出截断
- `com.agent.tool.code.ProcessRunner` — ProcessBuilder 封装
- `com.agent.tool.code.OutputTruncator` — 大输出截断

### Task 3: 验证
- "找所有含 TODO 的 Java 文件" -> GrepTool 返回结果
- `mvn --version` -> BashTool 返回版本号

---

## Stage 11: CLI REPL（交互式终端）

**Branch:** `stage-11`

### Task 1: JLine 终端
- `com.agent.cli.CliRepl` — 主 REPL 循环
- `com.agent.cli.InputReader` — JLine 3 封装 (历史、补全、多行输入)

### Task 2: 渲染与交互
- `com.agent.cli.TerminalRenderer` — Markdown -> ANSI 颜色
- `com.agent.cli.CommandHandler` — 斜杠命令 (/help, /clear, /exit, /cost)
- `com.agent.cli.Spinner` — 等待 LLM 时的动画

### Task 3: 验证
- 启动 -> 交互提示符; 输入消息 -> 彩色 Markdown 回复; `/cost` 显示费用

---

## Stage 12: 权限系统

**Branch:** `stage-12`

### Task 1: 权限模型
- `com.agent.permission.PermissionManager` — 规则管理
- `com.agent.permission.PermissionPolicy` 枚举: ALLOW, ASK, DENY
- `com.agent.permission.PermissionRule` — 每个工具的权限规则

### Task 2: 交互确认
- `com.agent.permission.InteractivePrompter` — CLI 确认提示
- `com.agent.permission.SessionAllowList` — "always" session 级白名单
- 默认: 读 -> ALLOW, 写 -> ASK, bash -> ASK

### Task 3: 验证
- 写文件 -> 弹出确认; 回答 "always" -> 后续不再询问

---

## Stage 13: 会话持久化

**Branch:** `stage-13`

### Task 1: Session 管理
- `com.agent.session.SessionManager` — 管理 `~/.my-agent/sessions/` JSON 文件
- `com.agent.session.Session` — 唯一 ID, 消息列表, 元数据
- `com.agent.session.SessionSerializer` — JSON 序列化/反序列化

### Task 2: CLI 集成
- 命令: `/save`, `/resume <id>`, `/sessions`, `/new`
- 每轮自动保存

### Task 3: 验证
- 聊 5 轮 -> 退出 -> 重启 -> `/resume` -> 上下文保留

---

## Stage 14: 流式输出

**Branch:** `stage-14`

### Task 1: SSE 流式客户端
- `com.agent.llm.claude.StreamingClaudeClient` — SSE 流式响应
- `com.agent.llm.model.StreamChunk` — 流块模型
- `com.agent.llm.ChunkAccumulator` — 块累积器

### Task 2: 流式 Agent Loop
- `com.agent.core.StreamingAgentLoop` — 流中检测 tool_use -> 暂停 -> 执行 -> 恢复
- `com.agent.cli.StreamRenderer` — 终端增量输出 (打字机效果)

### Task 3: 验证
- 提问 -> token 逐字出现; 需要工具时 -> 流暂停 -> 执行 -> 恢复

---

## Stage 15: 上下文压缩与裁剪

**Branch:** `stage-15`

### Task 1: 压缩策略
- `SlidingWindowCompressor` — 滑动窗口截断
- `SummaryCompressor` — LLM 生成摘要
- `ToolResultPruner` — 工具结果裁剪

### Task 2: 分层压缩管线
- `ContextCompressor` — 编排器
- `TieredCompression` — 按阈值触发不同策略
- `TokenEstimator` — Token 估算启发式

### Task 3: 验证
- 100 条消息 -> 压缩触发 -> token 数下降; 摘要后仍能回忆关键信息

---

## Stage 16: 上下文溢出恢复

**Branch:** `stage-16`

### Task 1: 溢出处理
- `ContextOverflowHandler` — 捕获 413/context_length_exceeded
- `PreflightTokenCheck` — 发送前估算
- `AggressiveCompressor` — 激进压缩
- `RecoveryStrategy` — 自动压缩 -> 重试管线

### Task 2: 验证
- 人为超限 -> 413 -> 自动恢复 -> 重试成功

---

## Stage 17: ReAct 循环（结构化推理）

**Branch:** `stage-17`

### Task 1: ReAct 实现
- `ReActAgent` — Thought -> Action -> Observation 循环
- `ReActParser` — 解析 LLM 输出格式
- `ReActPromptBuilder` — 构建 ReAct prompt
- `LoopDetector` — 检测重复循环

### Task 2: 验证
- 复杂查询 -> 显示 Thought 步骤; 格式违反 -> 优雅恢复

---

## Stage 18: 意图识别与路由

**Branch:** `stage-18`

### Task 1: 分类与路由
- `IntentClassifier` — 轻量分类 (用便宜模型)
- `IntentRouter` — 按意图激活不同工具集
- `Intent` 枚举: GENERAL, CODE, FILE, SEARCH 等
- `RoutingConfig` — 路由规则配置

### Task 2: 验证
- "查天气" -> GENERAL; "找所有 TODO" -> CODE -> 激活代码工具

---

## Stage 19: 基础 RAG

**Branch:** `stage-19`

### Task 1: RAG 管线
- `DocumentChunker` — 文档分块策略
- `EmbeddingClient` — 调用 Embedding API
- `VectorStore` — 内存向量存储 + 余弦相似度搜索
- `RagPipeline` — 检索 -> 注入 prompt
- `Chunk` — 文档块 POJO

### Task 2: 验证
- 索引 10 个文件 -> 问相关问题 -> 检索到正确 chunk

---

## Stage 20: 高级 RAG（多路召回、RRF、Rerank）

**Branch:** `stage-20`

### Task 1: 高级检索
- `BM25Retriever` — 关键词检索
- `RRFMerger` — Reciprocal Rank Fusion 融合
- `LlmReranker` — LLM 重排序
- `QueryRewriter` — HyDE 查询改写
- `AdvancedRagPipeline` — 编排所有检索策略

### Task 2: 验证
- 对比基础 RAG vs 高级 RAG -> 相关性提升

---

## Stage 21: MCP Client + 插件系统

**Branch:** `stage-21`

### Task 1: MCP 协议
- `McpClient` — MCP 协议客户端
- `McpTransport` — 通信层 (stdio/HTTP)
- `McpToolAdapter` — 将 MCP 工具适配为内部 Tool 格式
- `McpServerConfig` — MCP Server 配置

### Task 2: 插件加载
- `PluginLoader` — 动态加载外部 MCP Server 工具

### Task 3: 验证
- 配置 MCP Server -> 启动时发现并注册工具 -> Agent 透明使用

---

## Stage 22: SubAgent + Prompt 模板系统

**Branch:** `stage-22`

### Task 1: 子 Agent
- `SubAgent` — 独立上下文的子 Agent
- `AgentOrchestrator` — 委派与结果收集

### Task 2: 模板系统
- `PromptTemplate` — 模板定义
- `SkillRegistry` — LLM 触发的技能
- `CommandRegistry` — 用户触发的命令
- `TemplateRenderer` — 变量插值

### Task 3: 验证
- 复杂任务 -> 委派子 Agent -> 返回结果; `/review` 命令 -> 加载模板

---

## Stage 23: Web GUI（SSE + 聊天界面）

**Branch:** `stage-23`

### Task 1: 后端 API
- `com.agent.web.ChatController` — `POST /api/chat` (SSE streaming)
- `com.agent.web.WebSessionManager` — 多用户并发会话
- `GET /api/sessions`, `POST /api/sessions` 端点

### Task 2: 前端 UI
- `src/main/resources/static/index.html` — 聊天 UI
- 消息气泡、Markdown 渲染、代码高亮
- 工具执行进度展示、斜杠命令面板

### Task 3: 验证
- 浏览器打开 -> 聊天 UI -> 输入消息 -> 流式响应

---

## Stage 24: 高级任务规划

**Branch:** `stage-24`

### Task 1: 任务系统
- `TaskPlanner` — LLM 驱动任务分解
- `TaskDAG` — 有向无环图表示依赖
- `TaskExecutor` — 按依赖顺序执行
- `TaskState` — 子任务状态机
- `Replanner` — 失败后重规划

### Task 2: 验证
- 复杂任务 -> 分解为 4 个子任务 -> 并行执行 -> 失败时重规划

---

## Stage 25: 安全、沙箱与 Human-in-the-Loop

**Branch:** `stage-25`

### Task 1: 安全机制
- `InjectionDetector` — Prompt 注入检测
- `SandboxExecutor` — 沙箱执行 (资源限制)
- `RiskScorer` — 操作风险评分
- `HumanEscalation` — 置信度驱动的人工介入
- `OutputSanitizer` — 输出清洗

### Task 2: 验证
- 注入模式 -> 检测拦截; `rm -rf /` -> 沙箱阻止

---

## Stage 26: 生产工程化 & 毕业项目

**Branch:** `stage-26`

### Task 1: 持久化
- Spring Data JPA + H2/SQLite 持久化 session
- `SessionRepository` — 数据库存储
- `LongTermMemory` — 跨会话实体提取

### Task 2: 可观测性
- 结构化日志 + 关联 ID
- `MetricsCollector` — 指标收集

### Task 3: 评估与打包
- `AgentEvaluator` — 20 个测试用例评估套件
- `EvalTestCase` — 测试用例定义
- `mvn package` -> 可执行 JAR
- 完整 README

### Task 4: 验证
- 杀进程重启 -> session 不丢
- 评估套件 -> 报告成功率
- `java -jar agent.jar` -> 完整启动

---

## 全局约定

- **Java 版本**: 21
- **Mock 模式**: 所有 Stage 均支持 `--spring.profiles.active=mock` 无需真实 API key
- **分支策略**: 每个 Stage 一个 branch (`stage-01`, `stage-02`, ...)，从上一个 branch 创建
- **验证流程**: `mvn compile` -> `mvn spring-boot:run` (mock 模式) -> 功能演示
- **远程仓库**: `git@github.com:yytang26/my-agent-v2.git`
