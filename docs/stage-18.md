# Stage 18: 意图识别与路由

## 做了什么

- 轻量意图分类步骤（省钱避免无谓操作）
- 按意图激活不同工具集
- 可配置的路由规则（`RoutingConfig`）

新增/修改的关键类：
- `IntentClassifier` — 意图分类器，将用户消息映射到 `Intent` 枚举
- `IntentRouter` — 路由器，按意图返回对应工具列表
- `Intent` — 枚举：GENERAL, CODE, FILE, CONVERSATION
- `RoutingConfig` — 路由配置，维护 Intent → 工具名列表的映射

## 解决了什么问题

**核心问题**：每次调用都把所有工具传给 LLM，增加 token 消耗和误调风险。

**学习目标**：
- 意图分类的必要性和实现
- 按意图动态工具集的配置化
- 更便宜的模型做分类的思路

## 怎么做的

1. **意图分类**：`IntentClassifier.classify(userMessage)` 基于关键词匹配（CODE: "代码/搜索/grep", FILE: "文件/读取", GENERAL: 其他）
2. **路由配置**：`RoutingConfig` 定义 Intent → 工具名列表的映射，如 CODE → [grep, glob, bash, read_file]
3. **工具筛选**：`IntentRouter.route(userMessage)` → 分类意图 → 查配置获取工具名 → 从 `ToolRegistry` 获取 `ToolDefinition`
4. **降级策略**：`routingConfig.isEnabled()=false` 时返回所有工具（兼容模式）
5. **AgentLoop 集成**：`AgentLoop.run()` 开头调用 `intentRouter.route(userMessage)` 获取当轮工具列表

## 关键文件

- `src/main/java/com/agent/routing/IntentClassifier.java` — 意图分类器
- `src/main/java/com/agent/routing/IntentRouter.java` — 路由器
- `src/main/java/com/agent/routing/Intent.java` — 意图枚举
- `src/main/java/com/agent/routing/RoutingConfig.java` — 路由配置

## 验证方式

1. "查天气" → Intent.GENERAL → 最少工具集
2. "找所有 TODO" → Intent.CODE → 激活 grep/glob/bash 等代码工具
3. 关闭路由 → 所有工具都传给 LLM
