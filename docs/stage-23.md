# Stage 23: Web GUI（SSE + 聊天界面）

## 做了什么

- Spring Boot REST + SSE 端点
- HTML/CSS/JS 聊天 UI（支持 Markdown 渲染、代码高亮）
- 多用户并发会话管理
- Web 版 Agent Loop（`WebAgentLoop`）

新增/修改的关键类：
- `ChatController` — REST 控制器，`POST /api/chat` (SSE)、`GET /api/sessions`、`POST /api/sessions`
- `WebAgentLoop` — Web 版 Agent 循环，SSE 流式推送
- `WebSessionManager` — Web 会话管理，支持多用户并发
- `WebConfig` — Web 配置（CORS、静态资源）
- `ChatRequest` — 请求 DTO
- `MessageDto` — 消息 DTO
- `WebSessionInfo` — 会话信息 DTO
- `src/main/resources/static/` — 前端文件（HTML/CSS/JS）

## 解决了什么问题

**核心问题**：Agent 仅能通过 CLI 使用，非技术用户无法使用，无法多用户并发。

**学习目标**：
- Spring WebFlux SSE 流式推送
- 前后端分离的聊天架构
- 多会话并发管理

## 怎么做的

1. **SSE 端点**：`POST /api/chat` 返回 `Flux<ServerSentEvent<String>>`，逐 token 推送到浏览器
2. **WebAgentLoop**：类似 `StreamingAgentLoop`，但通过 `SseEmitter` / `Flux` 推送而非终端渲染
3. **会话管理**：`WebSessionManager` 用 `ConcurrentHashMap<String, Session>` 管理多用户会话
4. **前端 UI**：`index.html` 实现消息气泡、Markdown 渲染（marked.js）、代码高亮（highlight.js）
5. **CORS 配置**：`WebConfig` 允许前端跨域访问
6. **双模式启动**：`Application` 支持 `agent.mode=web|cli|both`，Web 模式启动 HTTP 服务

## 关键文件

- `src/main/java/com/agent/web/ChatController.java` — REST 控制器
- `src/main/java/com/agent/web/WebAgentLoop.java` — Web Agent 循环
- `src/main/java/com/agent/web/WebSessionManager.java` — 会话管理
- `src/main/java/com/agent/web/WebConfig.java` — Web 配置
- `src/main/resources/static/index.html` — 聊天 UI
- `src/main/resources/static/css/` — 样式
- `src/main/resources/static/js/` — 前端逻辑

## 验证方式

1. `mvn spring-boot:run` → 浏览器打开 `http://localhost:8080`
2. 聊天 UI → 输入消息 → 流式响应
3. 多个浏览器标签 → 各自独立会话
