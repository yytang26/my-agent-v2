# Stage 06: 对话记忆

## 做了什么

- 实现 `ConversationMemory` 接口 + `InMemoryConversationMemory` 实现
- 系统提示词注入到消息列表 position 0
- 当前上下文的 token 估算
- 不可变消息记录（`Message` record）

新增/修改的关键类：
- `ConversationMemory` — 接口：addMessage, getMessages, clear, setSystemPrompt
- `InMemoryConversationMemory` — ArrayList 实现，维护 systemPrompt + messages
- `Message` — 不可变 record（role, content），提供 `user()` / `assistant()` 静态工厂
- `MessageRole` — 枚举：SYSTEM, USER, ASSISTANT, TOOL

## 解决了什么问题

**核心问题**：没有记忆的 LLM 调用是无状态的，每次对话都是全新的，无法感知上下文。

**学习目标**：
- 理解对话历史的消息列表结构（system → [user, assistant, ...]）
- 认识到上下文无限增长的代价（token 费用 + 窗口限制）
- 为后续压缩打下基础

## 怎么做的

1. **接口抽象**：`ConversationMemory` 定义 `addMessage()`, `getMessages()`, `clear()`, `setSystemPrompt()` 等方法
2. **内存实现**：`InMemoryConversationMemory` 用 `List<Message>` 存储消息，`systemPrompt` 单独管理
3. **消息设计**：`Message` 使用 Java record 保证不可变性，`MessageRole` 枚举区分角色
4. **Token 估算**：`getMessageCount()` 和后续 `TokenEstimator` 为压缩提供依据
5. **ChatMessage 适配**：`addChatMessage(ChatMessage)` 支持从 LLM 层的消息格式转换

## 关键文件

- `src/main/java/com/agent/memory/ConversationMemory.java` — 接口
- `src/main/java/com/agent/memory/InMemoryConversationMemory.java` — ArrayList 实现
- `src/main/java/com/agent/memory/Message.java` — 不可变消息 record
- `src/main/java/com/agent/memory/MessageRole.java` — 角色枚举

## 验证方式

1. 发送 5 条消息到 `ConversationMemory`
2. 调用 `getMessages()` → 返回包含所有历史消息的列表
3. 将完整消息列表传给 LLM → LLM 能感知前文语境
