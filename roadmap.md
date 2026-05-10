
> 目标：从零开始构建一个可玩的、理解原理的 AI Agent。
---

## 基础阶段：从裸调 API 到可交互的 Agent（12 阶段）

### 阶段 1：最基础的 HTTP Client（30 分钟）
**目标**：裸调一个 LLM API，理解最基础的请求-响应模式。
- 用 `curl` 或 Python `requests` 直接调 OpenAI/Claude API
- 理解 `messages` 数组格式、`system` prompt、`temperature`/`max_tokens`
- 手写一个函数：`ask_llm(prompt) -> response`
- **关键问题**：latency 在哪？token 计费怎么算？

### 阶段 2：多 LLM Provider 切换（1 小时）
**目标**：抽象掉"调用哪家 LLM"的细节。
- 定义一个 `BaseLLM` 接口：`chat(messages, model) -> str`
- 实现 `OpenAIClient`、`ClaudeClient`、`DeepSeekClient`
- 加一个简单工厂或字典映射，通过配置切换模型
- 支持**运行时热切换**，不重启服务换模型
- **关键问题**：不同模型的消息格式差异、工具调用格式差异、error handling 差异

### 阶段 3：工具调用（Tools / Functions）（2 小时）
**目标**：让 LLM 能"动手"。
- 手动写一个 `get_weather(city) -> str` 函数
- 理解 function calling 的 schema 定义（JSON Schema）
- 实现循环：`LLM 请求 → 判断是否需要工具 → 执行工具 → 把结果塞回 messages → 再请求 LLM`
- **关键问题**：tool call 的 message 格式、并行 tools 的处理、工具执行失败怎么回传

### 阶段 3.5：意图识别与路由（1 小时）
**目标**：不做无用功，把请求分发到正确的处理管道。
- 用一个轻量 prompt 做前置分类（如 `RAG / GENERAL` / `CODE`）
- 根据意图决定是否触发昂贵的向量检索、是否走代码生成管道
- **关键问题**：分类不准怎么办？延迟与准确率的 trade-off

### 阶段 4：Memory（记忆）（2 小时）
**目标**：Agent 能记住对话历史。
- 最简单的记忆：一个 `list[message]`，每次对话追加
- 理解为什么上下文会"爆"（token limit）
- **关键问题**：无限增长怎么办？消息太多导致 API 报错、又贵又慢

### 阶段 5：上下文压缩与裁剪（3 小时）
**目标**：解决记忆爆炸问题。
- **滑动窗口**：只保留最近 N 条消息
- **Token 裁剪**：用 tiktoken / 粗略估算，控制在 max_tokens 内
- **摘要压缩**：把老对话丢给 LLM 生成 `summary`，替换原始消息
- **关键问题**：摘要丢失了多少信息？什么该保留、什么该丢掉？
- **参考**：`aiagentdemo` 的三层递进压缩（摘要 → Assistant 裁剪 → 滑动窗口）

### 阶段 6：ReAct 循环（3 小时）
**目标**：让 Agent 能"思考 → 行动 → 观察 → 再思考"。
- 不用原生 function calling，改用纯文本实现 ReAct 格式：
  ```
  Thought: 我需要搜索天气
  Action: search_weather{"city": "北京"}
  Observation: 晴天 25°C
  Thought: ...
  ```
- 自己写 parser 提取 Thought/Action/Observation
- **关键问题**：LLM 不遵守格式怎么办？循环次数怎么限制？死循环怎么检测# Agent 学习路线图（22 阶段）

> 目标：从零开始构建一个可玩的、理解原理的 AI Agent。
> 参考项目：`aiagentdemo`（Java / Spring AI）

---

## 基础阶段：从裸调 API 到可交互的 Agent（12 阶段）

### 阶段 1：最基础的 HTTP Client（30 分钟）
**目标**：裸调一个 LLM API，理解最基础的请求-响应模式。
- 用 `curl` 或 Python `requests` 直接调 OpenAI/Claude API
- 理解 `messages` 数组格式、`system` prompt、`temperature`/`max_tokens`
- 手写一个函数：`ask_llm(prompt) -> response`
- **关键问题**：latency 在哪？token 计费怎么算？

### 阶段 2：多 LLM Provider 切换（1 小时）
**目标**：抽象掉"调用哪家 LLM"的细节。
- 定义一个 `BaseLLM` 接口：`chat(messages, model) -> str`
- 实现 `OpenAIClient`、`ClaudeClient`、`DeepSeekClient`
- 加一个简单工厂或字典映射，通过配置切换模型
- 支持**运行时热切换**，不重启服务换模型
- **关键问题**：不同模型的消息格式差异、工具调用格式差异、error handling 差异

### 阶段 3：工具调用（Tools / Functions）（2 小时）
**目标**：让 LLM 能"动手"。
- 手动写一个 `get_weather(city) -> str` 函数
- 理解 function calling 的 schema 定义（JSON Schema）
- 实现循环：`LLM 请求 → 判断是否需要工具 → 执行工具 → 把结果塞回 messages → 再请求 LLM`
- **关键问题**：tool call 的 message 格式、并行 tools 的处理、工具执行失败怎么回传

### 阶段 3.5：意图识别与路由（1 小时）
**目标**：不做无用功，把请求分发到正确的处理管道。
- 用一个轻量 prompt 做前置分类（如 `RAG / GENERAL` / `CODE`）
- 根据意图决定是否触发昂贵的向量检索、是否走代码生成管道
- **关键问题**：分类不准怎么办？延迟与准确率的 trade-off

### 阶段 4：Memory（记忆）（2 小时）
**目标**：Agent 能记住对话历史。
- 最简单的记忆：一个 `list[message]`，每次对话追加
- 理解为什么上下文会"爆"（token limit）
- **关键问题**：无限增长怎么办？消息太多导致 API 报错、又贵又慢

### 阶段 5：上下文压缩与裁剪（3 小时）
**目标**：解决记忆爆炸问题。
- **滑动窗口**：只保留最近 N 条消息
- **Token 裁剪**：用 tiktoken / 粗略估算，控制在 max_tokens 内
- **摘要压缩**：把老对话丢给 LLM 生成 `summary`，替换原始消息
- **关键问题**：摘要丢失了多少信息？什么该保留、什么该丢掉？
- **参考**：`aiagentdemo` 的三层递进压缩（摘要 → Assistant 裁剪 → 滑动窗口）

### 阶段 6：ReAct 循环（3 小时）
**目标**：让 Agent 能"思考 → 行动 → 观察 → 再思考"。
- 不用原生 function calling，改用纯文本实现 ReAct 格式：
  ```
  Thought: 我需要搜索天气
  Action: search_weather{"city": "北京"}
  Observation: 晴天 25°C
  Thought: ...
  ```
- 自己写 parser 提取 Thought/Action/Observation
- **关键问题**：LLM 不遵守格式怎么办？循环次数怎么限制？死循环怎么检测？

### 阶段 7：基础 RAG（检索增强生成）（4 小时）
**目标**：Agent 能读自己的"书"。
- 不要急着上向量数据库，先用最简单的方式：
  - 把文档切成 chunk，存入 list，检索时遍历做关键词匹配
- 然后升级：用 embedding 模型 + `faiss` / `chromadb` 做向量检索
- 检索 top-k chunks，拼进 system prompt
- **关键问题**：chunk 大小怎么定？retrieval 准不准？

### 阶段 7.5：RAG 工程深化（4 小时）
**目标**：体验"召回 vs 精排"的分工，理解为什么高级 RAG 有这么多组件。
- **多种分块策略**：固定大小、段落、句子、滑动窗口、语义分块——对比效果
- **多路召回**：语义检索 + BM25 关键词检索 + 查询改写检索
- **RRF 融合**：用 Reciprocal Rank Fusion 合并不同路的结果
- **Rerank 重排**：召回 9 个，用 cross-encoder / LLM 挑出最相关的 2-3 个
- **关键问题**：召回率 vs 精确率怎么 balance？每加一个组件 latency 涨多少？
- **参考**：`aiagentdemo` 的完整 RAG 流水线

### 阶段 8：自定义工具 + MCP + SubAgent（5 小时）
**目标**：能力模块化，Agent 能指挥 Agent。
- **自定义工具**：把前几阶段的工具整理成可插拔的 `Tool` 类，自动扫描注册
- **MCP**：接入 Model Context Protocol，让外部服务给你的 Agent 提供工具/资源
- **SubAgent**：一个主 Agent 把任务拆解，派给子 Agent 执行，子 Agent 返回结果
- **关键问题**：SubAgent 的上下文怎么传递？多个 Agent 共享记忆还是独立？MCP 的 discovery 和权限怎么管？

### 阶段 8.5：Prompt 模板系统（3 小时）
**目标**：理解"把 prompt 产品化"的两种模态。
- **Skill**：Markdown + YAML Front Matter 驱动，LLM 自主根据 `description` 决定是否调用
- **Command**：纯 Markdown 模板，用户主动输入 `/命令名` 触发
- 实现 `{{input}}`、`{{variable}}` 占位符替换
- **关键问题**：用户触发 vs LLM 自主触发的架构差异是什么？什么时候用哪种？
- **参考**：`aiagentdemo` 的 Skill/Command 双系统设计

### 阶段 9：Prompt 工程与系统提示词（持续迭代）
**目标**：让 Agent 聪明、可控、不跑偏。
- 精心打磨 system prompt：角色定义、输出格式约束、安全边界
- Few-shot prompting：给 LLM 看几个正确执行的例子
- 链式思考（Chain-of-Thought）：让 LLM 先想后答
- **关键问题**：prompt 越长成本越高；prompt 被越狱怎么防？

### 阶段 10：流式输出与简易 Web UI（4 小时）
**目标**：从"黑盒等结果"到"实时看思考过程的交互体验"。
- **SSE 流式**：用 Server-Sent Events 逐字推送 LLM 输出
- **前端**：原生 HTML + JS 做一个聊天界面，支持 Markdown 渲染、命令面板 `/`、会话切换
- **关键问题**：流式输出时工具调用怎么展示？Agent Loop 的中间过程要不要给用户看？
- **参考**：`aiagentdemo` 的前端交互设计

---

## 进阶阶段：从"能玩"到"能打"（10 阶段）

### 阶段 11：高级任务规划（6 小时）
**目标**：Agent 不再只是"一步一想"，而是能提前规划、回溯、并行。
- 手写一个**任务分解器**：给定复杂任务，输出 `subtask_1 → subtask_2 ∧ subtask_3` 格式的 DAG
- 实现简单的**状态机**：每个子任务有 `pending / running / success / failed` 状态
- 支持**重规划**：某个子任务失败后，Agent 能重新生成剩余计划
- **关键问题**：规划幻觉怎么办？循环依赖怎么检测？

### 阶段 12：长期记忆与知识图谱（8 小时）
**目标**：Agent 拥有跨越会话的"人生经历"。
- 把对话中的**实体和关系**提取出来，存入图数据库
- 实现**记忆检索**："我最近和谁聊过关于 Agent 的事？"
- 记忆**分层**：工作记忆 → 短期记忆 → 长期记忆
- **关键问题**：记忆冲突怎么更新？记忆太多检索变慢？隐私记忆怎么隔离？

### 阶段 13：人在回路（Human-in-the-Loop）（4 小时）
**目标**：Agent 知道什么时候该找人类确认。
- **置信度判断**：LLM 对回答的不确定性打分，低于阈值时暂停询问
- **敏感操作拦截**：涉及删除、发邮件等操作前必须人工确认
- **中断与恢复**：人类介入后从中断点继续而非重来
- **关键问题**：太频繁会烦人，太少会闯祸，怎么 balance？

### 阶段 14：Agent 评估体系（8 小时）
**目标**：从"感觉挺聪明"到"可量化地聪明"。
- **指标定义**：任务成功率、平均步数、LLM 调用次数、延迟、token 成本
- **LLM-as-a-Judge**：用更强的模型给 Agent 回答打分
- **基准测试集**：20-50 个固定测试用例，每次改代码后跑一遍
- **关键问题**：评估指标和用户体验不一致怎么办？

### 阶段 15：高级 RAG 持续优化（4 小时）
**目标**：在阶段 7.5 基础上继续深化。
- **查询改写**：HyDE、Query Expansion
- **上下文压缩**：把检索到的长文档压缩后再塞进 prompt
- **重排序微调**：对比 cross-encoder vs LLM 打分的效果和成本

### 阶段 16：多模态 Agent（8 小时）
**目标**：Agent 不只能读文本，还能看图、听语音、处理表格。
- 接入**视觉模型**：分析图片内容
- **结构化数据**：读取 CSV / JSON，执行过滤、聚合
- **统一表示**：把多模态内容转成统一嵌入
- **关键问题**：不同模态的 token 成本差异巨大；视觉幻觉更严重

### 阶段 17：安全与沙箱（6 小时）
**目标**：Agent 能办事，但不会办坏事。
- **Prompt 防御**：识别拒绝注入攻击
- **工具沙箱**：用 subprocess + 资源限制或 Docker 隔离
- **权限最小化**：每个工具的访问范围明确
- **输出审查**：敏感信息、有害内容过滤

### 阶段 18：生产级工程化（10 小时）
**目标**：从"本地玩具"到"可部署服务"。
- **持久化**：用 SQLite / PostgreSQL 存会话状态，重启不丢失
- **并发架构**：FastAPI / Spring Boot + async，多用户同时对话
- **运行时热更新**：改 prompt / 换模型 / 接新工具 不用重启
- **监控埋点**：token 消耗、latency、工具调用次数的全链路追踪
- **关键问题**：并发下共享资源怎么锁？长时任务怎么防超时？

### 阶段 19：自主 Agent 与目标驱动（6 小时）
**目标**：体验"设定目标后让它自己跑"的边界。
- 实现**目标分解循环**：用户给目标 → Agent 生成里程碑 → 自主执行 → 自检完成度
- **自检机制**：定期问自己"我离目标还有多远？"
- **关键问题**：为什么现在业界更推崇"有界自主"而非完全自主？

### 阶段 20：毕业项目——构建你的个人超级助手（20+ 小时）
**目标**：把前 19 个阶段整合成一个你**真的会用**的 Agent。
- 功能自选：编程助手、论文阅读助手、生活管家
- **硬性要求**：多 LLM 路由、RAG、工具调用、长期记忆、评估体系
- 部署到你自己能访问的环境，持续使用并迭代
- 写一份完整的技术文档/博客，讲清你的架构和取舍
- **关键问题**：你为什么这里用这个方案而不是那个？

---

## 学习建议

1. **每阶段结束后写 `NOTES.md`**：解决了什么问题？核心结构图？遇过什么坑？trade-off 是什么？
2. **对比学习**：每做完一个阶段，对照参考项目 `aiagentdemo` 的对应模块，看实现的优劣差异。
3. **先跑通，再优化**：不要在一开始就追求和参考项目一样的工程化程度。先让它能用，再拆 engine。
4. **毕业项目不是终点**：走完这 22 个阶段，你对 Agent 原理、实现、瓶颈的认知会远超"只调库的使用者"。

---

## 参考项目能力覆盖对照表

| 参考项目模块 | 对应本路线阶段 | 评价 |
|-------------|-------------|------|
| AgentCore 编排 | 阶段 3 + 6 + 10 | 直接使用 Spring AI 内置循环 |
| ChatMemory 三层压缩 | 阶段 4 + 5 | 工程化程度很高 |
| IntentRecognizer | 阶段 3.5 | 经典前置优化 |
| RAG 全流水线 | 阶段 7 + 7.5 | 6 种分块 + 3 路召回 + RRF + Rerank |
| Skill / Command | 阶段 8.5 | 双模态触发设计精妙 |
| SubAgent | 阶段 8 | 记忆隔离实现干净 |
| MCP Client/Server | 阶段 8 | 自动协议适配 + 持久化 |
| 流式输出 SSE | 阶段 10 | 前端交互体验完整 |
| 多模型运行时切换 | 阶段 2 | 参考项目已包含 |

**项目没有、但进阶路线覆盖的能力**：长期记忆/知识图谱、人在回路、评估体系、安全沙箱、多模态、自主目标驱动。