# Stage 11: CLI REPL（交互式终端）

## 做了什么

- 用 JLine 3 构建交互式 REPL 终端
- 命令历史、自动补全、多行输入
- 终端 Markdown 渲染（颜色、代码块）
- 等待 LLM 时的 Spinner 动画
- 斜杠命令系统：`/help`, `/clear`, `/exit`, `/cost`, `/mode`

新增/修改的关键类：
- `CliRepl` — 主 REPL 循环，支持 CLI / Web / Both 三种启动模式
- `TerminalRenderer` — Markdown → ANSI 颜色渲染器
- `CommandHandler` — 斜杠命令调度（/help, /clear, /exit, /cost, /mode, /save, /resume, /sessions, /new）
- `InputReader` — JLine LineReader 封装，支持多行输入和历史
- `Spinner` — 等待动画（⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏）

## 解决了什么问题

**核心问题**：Agent 只能通过代码调用，没有交互式界面，用户体验差。

**学习目标**：
- JLine 3 终端交互库的使用
- Markdown → ANSI 颜色渲染
- 异步 Spinner + 同步 LLM 调用的线程协调

## 怎么做的

1. **REPL 主循环**：`CliRepl.start()` 进入 `while(true)` 循环，`inputReader.readLine()` 等待输入
2. **命令分发**：`commandHandler.isCommand(input)` 检测 `/` 前缀 → 路由到对应处理方法
3. **Agent 模式切换**：`/mode react|function` 切换 AgentLoop / ReActAgent / StreamingAgentLoop
4. **Spinner 动画**：独立线程显示动画，LLM 调用前 `spinner.start()`，结束后 `spinner.stop()`
5. **Application 集成**：`@ConditionalOnExpression` 根据 `agent.mode` 决定是否启动 CLI
6. **Markdown 渲染**：`TerminalRenderer.render()` 将 Markdown 文本转为 ANSI 颜色码（代码块、粗体、标题）

## 关键文件

- `src/main/java/com/agent/cli/CliRepl.java` — 主 REPL 循环
- `src/main/java/com/agent/cli/TerminalRenderer.java` — Markdown → ANSI
- `src/main/java/com/agent/cli/CommandHandler.java` — 命令调度
- `src/main/java/com/agent/cli/InputReader.java` — JLine 封装
- `src/main/java/com/agent/cli/Spinner.java` — 等待动画

## 验证方式

1. `mvn spring-boot:run -Dagent.mode=cli` → 交互提示符出现
2. 输入消息 → 彩色 Markdown 回复
3. `/cost` → 显示费用摘要
4. `/help` → 列出所有命令
