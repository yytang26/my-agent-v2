# Stage 21: MCP Client + 插件系统

## 做了什么

- 实现 MCP (Model Context Protocol) 协议客户端
- 支持 Stdio 和 HTTP 两种传输方式
- 连接外部 MCP Server 动态发现和调用工具
- 插件加载机制（`PluginLoader`）
- MCP 工具适配为内部 `ToolDefinition`（`McpToolAdapter`）

新增/修改的关键类：
- `McpClient` — MCP 客户端，实现 JSON-RPC 2.0 协议通信
- `McpTransport` — 传输层接口
- `StdioTransport` — Stdio 传输（启动子进程通信）
- `HttpTransport` — HTTP SSE 传输
- `McpToolAdapter` — 将 MCP 工具适配为 `ToolDefinition` 和动态 `ToolExecutor`
- `PluginLoader` — 插件加载器，启动时初始化所有配置的 MCP Server
- `McpServerConfig` — MCP Server 配置
- `AgentMcpProperties` — Spring Boot 配置属性类

## 解决了什么问题

**核心问题**：工具集硬编码在项目内，无法动态扩展，不支持外部工具服务。

**学习目标**：
- MCP 协议的 JSON-RPC 2.0 通信模式
- Stdio vs HTTP 两种传输方式
- 动态工具发现与注册

## 怎么做的

1. **协议通信**：`McpClient` 实现 JSON-RPC 2.0，`sendRequest(method, params)` 发送请求，`sendNotification(method, params)` 发送通知
2. **初始化握手**：`initialize()` 发送 `protocolVersion: "2024-11-05"` + `clientInfo` → 收到 server capabilities
3. **工具发现**：`listTools()` 调用 `tools/list` → 解析返回的工具列表（name, description, inputSchema）
4. **工具调用**：`callTool(name, arguments)` 调用 `tools/call` → 返回工具执行结果
5. **McpToolAdapter**：将 MCP 工具的 `inputSchema` 转换为内部 `ToolDefinition`，执行时委托给 `McpClient.callTool()`
6. **PluginLoader**：启动时读取 `AgentMcpProperties` 配置 → 逐个初始化 MCP Server → 注册适配后的工具到 `ToolRegistry`

## 关键文件

- `src/main/java/com/agent/mcp/McpClient.java` — MCP 客户端
- `src/main/java/com/agent/mcp/McpTransport.java` — 传输层接口
- `src/main/java/com/agent/mcp/StdioTransport.java` — Stdio 传输
- `src/main/java/com/agent/mcp/HttpTransport.java` — HTTP 传输
- `src/main/java/com/agent/mcp/McpToolAdapter.java` — 工具适配器
- `src/main/java/com/agent/mcp/PluginLoader.java` — 插件加载器
- `src/main/java/com/agent/mcp/McpServerConfig.java` — Server 配置
- `src/main/java/com/agent/mcp/AgentMcpProperties.java` — Spring 配置属性

## 验证方式

1. 配置 MCP Server（`application.yml` 中 `agent.mcp.servers`）
2. 启动时日志显示 "MCP server [name] initialized"
3. Agent 透明使用 MCP 工具（与内置工具无差异）
