# Stage 17: ReAct 循环（结构化推理）

## 做了什么

- 实现 Thought → Action → Observation 推理模式
- `ReActParser` 解析 LLM 输出中的 Thought / Action / Answer 块
- `LoopDetector` 检测循环重复动作
- 格式违反时的容错与纠正
- `/mode react` CLI 命令切换 ReAct 模式

新增/修改的关键类：
- `ReActAgent` — ReAct 模式 Agent，`run(userMessage) → AgentResponse`
- `ReActParser` — 解析 Thought / Action / Answer，提取工具名和参数
- `ReActPromptBuilder` — 构建 ReAct 格式的系统提示词
- `LoopDetector` — 检测连续重复的 Action，提示换策略
- `AgentMode` — Agent 模式管理（function / react）

## 解决了什么问题

**核心问题**：原生 function calling 模式下 LLM "隐式思考"，推理过程不透明，难以调试。

**学习目标**：
- ReAct 推理模式 vs 原生 function calling 的差异
- 纯文本格式的 Thought/Action/Observation 循环
- 格式违反时的容错策略
- 循环检测与策略切换

## 怎么做的

1. **系统提示词**：`ReActPromptBuilder` 构建要求 LLM 严格输出 `Thought: ... Action: tool(params)` 或 `Answer: ...` 格式的 prompt
2. **解析器**：`ReActParser.parse(llmOutput)` 用正则提取 Thought、Action、Answer 字段
3. **推理循环**：LLM 输出 Action → 解析工具名和参数 → 执行工具 → 将 Observation 追加到 context → 继续循环
4. **格式纠正**：`parser.isValidFormat()` 检测违规 → 追加纠正提示 "请严格按照格式输出"
5. **循环检测**：`LoopDetector.recordAction(action)` 记录最近动作 → 连续相同动作超过阈值 → 提示 "请尝试换一种策略"
6. **模式切换**：`AgentMode` + `CliRepl` 的 `/mode react` 命令切换到 ReAct 模式

## 关键文件

- `src/main/java/com/agent/react/ReActAgent.java` — ReAct Agent
- `src/main/java/com/agent/react/ReActParser.java` — 输出解析器
- `src/main/java/com/agent/react/ReActPromptBuilder.java` — 提示词构建
- `src/main/java/com/agent/react/LoopDetector.java` — 循环检测
- `src/main/java/com/agent/react/AgentMode.java` — 模式管理

## 验证方式

1. `/mode react` → 切换到 ReAct 模式
2. 复杂查询 → 显示 Thought 步骤
3. 格式违反 → 优雅恢复（追加纠正提示）
4. 重复动作 → LoopDetector 提示换策略
