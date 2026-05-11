# Stage 22: SubAgent + Prompt 模板系统

## 做了什么

- 子 Agent 委派（独立上下文，不污染主对话）
- Skill（LLM 触发）/ Command（用户触发）模板系统
- 模板变量插值（`TemplateRenderer`）
- 并行子 Agent 执行（`AgentOrchestrator.delegateParallel()`）

新增/修改的关键类：
- `SubAgent` — 子 Agent，拥有独立的 memory 和 system prompt
- `AgentOrchestrator` — 编排器，创建和管理子 Agent，支持并行委派
- `PromptTemplate` — 提示词模板（name, description, template, variables）
- `SkillRegistry` — Skill 注册表，LLM 可触发的技能模板
- `CommandRegistry` — Command 注册表，用户可触发的命令模板
- `TemplateRenderer` — 模板变量插值，`${variable}` 替换

## 解决了什么问题

**核心问题**：单一 Agent 处理所有任务，复杂任务难以分解，上下文互相干扰。

**学习目标**：
- 子 Agent 委派模式（独立上下文隔离）
- Skill vs Command 的区别与设计
- 模板变量插值的实现
- 并行子任务执行

## 怎么做的

1. **SubAgent**：独立持有 `ConversationMemory`，拥有自己的 system prompt 和工具集，`run(task) → AgentResponse`
2. **编排器**：`AgentOrchestrator.createSubAgent(name, systemPrompt)` 创建子 Agent，`delegate(task, systemPrompt)` 委派任务
3. **并行委派**：`delegateParallel(tasks)` 用 `CompletableFuture.supplyAsync()` 并行执行多个子 Agent
4. **PromptTemplate**：模板包含 `${variable}` 占位符，`TemplateRenderer.render(template, variables)` 替换
5. **SkillRegistry**：LLM 通过工具调用触发 Skill（如 `use_skill(name, args)`）
6. **CommandRegistry**：用户通过 `/command` 触发（如 `/review` → 加载 code review 模板）

## 关键文件

- `src/main/java/com/agent/subagent/SubAgent.java` — 子 Agent
- `src/main/java/com/agent/subagent/AgentOrchestrator.java` — 编排器
- `src/main/java/com/agent/template/PromptTemplate.java` — 模板定义
- `src/main/java/com/agent/template/SkillRegistry.java` — Skill 注册表
- `src/main/java/com/agent/template/CommandRegistry.java` — Command 注册表
- `src/main/java/com/agent/template/TemplateRenderer.java` — 模板渲染
- `src/main/resources/skills/` — Skill 模板 YAML 文件

## 验证方式

1. 复杂任务 → 委派子 Agent → 返回结果，主对话不受干扰
2. `/review` → 加载 code review 模板 → 执行分析
3. 并行委派多个子任务 → 结果合并
