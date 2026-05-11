# Stage 13: 会话持久化

## 做了什么

- 对话状态 JSON 序列化到磁盘
- 按 ID 恢复历史会话
- 自动保存（每轮对话后）
- CLI 命令：`/save`, `/resume <id>`, `/sessions`, `/new`

新增/修改的关键类：
- `SessionManager` — 管理 `~/.my-agent/sessions/` 下的 JSON 文件
- `Session` — 会话实体（id, metadata, messages）
- `SessionSerializer` — Jackson JSON 序列化/反序列化
- `SessionMetadata` — 会话元数据 record（id, title, createdAt, lastUpdatedAt, messageCount, model）

## 解决了什么问题

**核心问题**：对话历史仅存内存，进程退出即丢失，无法跨会话延续工作。

**学习目标**：
- 对话状态的 JSON 序列化与反序列化
- 文件系统持久化的最佳实践
- 自动保存机制防止崩溃丢失

## 怎么做的

1. **Session 结构**：每个 session 包含唯一 UUID、`SessionMetadata`（标题、时间戳、消息数）和 `List<Message>`
2. **文件存储**：`~/.my-agent/sessions/{sessionId}.json`，启动时自动创建目录
3. **自动保存**：`AgentLoop` 每轮结束后调用 `sessionManager.autoSave()` → 从 `ConversationMemory` 拉取最新消息 → 写入 JSON
4. **恢复会话**：`resume(sessionId)` → 从 JSON 反序列化 → 清空内存 → 逐条恢复消息到 `ConversationMemory`
5. **列表查询**：`listSessions()` → 遍历目录下所有 `.json` → 按 `lastUpdatedAt` 倒序排列
6. **标题生成**：`updateTitleFromFirstUserMessage()` 自动从第一条用户消息提取会话标题

## 关键文件

- `src/main/java/com/agent/session/SessionManager.java` — 会话管理器
- `src/main/java/com/agent/session/Session.java` — 会话实体
- `src/main/java/com/agent/session/SessionSerializer.java` — JSON 序列化
- `src/main/java/com/agent/session/SessionMetadata.java` — 元数据 record

## 验证方式

1. 聊 5 轮 → 退出
2. 重启 → `/sessions` 列出历史会话
3. `/resume <id>` → 上下文恢复，继续对话
