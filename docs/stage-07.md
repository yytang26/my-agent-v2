# Stage 07: Tool 框架（注解驱动注册）

## 做了什么

- 用 Java 注解 `@Tool` / `@ToolParam` 实现可插拔的工具注册
- `ToolRegistry` 扫描 Spring Bean，自动发现所有 `@Tool` 注解方法
- 从注解自动生成 JSON Schema（`ToolDefinition`）
- `ToolExecutor` 按名称调用工具，处理参数转换和结果包装
- Demo 工具：`get_current_time()`

新增/修改的关键类：
- `@Tool` / `@ToolParam` — 注解，标注工具方法和参数
- `ToolRegistry` — Spring Bean 扫描 + 注册表，维护 `Map<String, ToolMethod>`
- `ToolDefinition` — 工具的 JSON Schema 表示（name, description, parameters）
- `ToolExecutor` — 按名称执行工具，处理权限检查和结果包装
- `ToolResult` — 工具执行结果（content, isError）
- `GetCurrentTimeTool` — 示例工具

## 解决了什么问题

**核心问题**：Agent 需要与外部世界交互，但没有统一的工具注册和调用机制。

**学习目标**：
- 用注解 + 反射实现声明式工具注册（类似 Spring MVC 的 `@RequestMapping`）
- 理解 Claude 的 `tool_use` / `tool_result` 消息格式
- 从 Java 方法签名自动生成 JSON Schema

## 怎么做的

1. **注解定义**：`@Tool(name, description)` 标注方法，`@ToolParam(name, description, required)` 标注参数
2. **自动发现**：`ToolRegistry` 在 `@PostConstruct` 时扫描所有 Spring Bean，用反射找到 `@Tool` 方法
3. **Schema 生成**：从方法注解自动构建 `ToolDefinition`（Claude `tools` 格式），包含 `input_schema`
4. **执行调度**：`ToolExecutor.execute(toolName, toolUseId, arguments)` → 查找注册表 → 反射调用 → 返回 `ToolResult`
5. **权限集成**：`ToolExecutor` 在执行前调用 `PermissionManager.checkPermission()`

## 关键文件

- `src/main/java/com/agent/tool/Tool.java` — `@Tool` 注解
- `src/main/java/com/agent/tool/ToolParam.java` — `@ToolParam` 注解
- `src/main/java/com/agent/tool/ToolRegistry.java` — 工具注册表
- `src/main/java/com/agent/tool/ToolDefinition.java` — Schema 表示
- `src/main/java/com/agent/tool/ToolExecutor.java` — 执行器
- `src/main/java/com/agent/tool/ToolResult.java` — 结果包装
- `src/main/java/com/agent/tool/builtin/` — 内置工具（GetCurrentTimeTool 等）

## 验证方式

1. 启动时日志显示 `@Tool` 注解方法被发现
2. 发送需要工具调用的 prompt → Agent 自动调用工具 → 结果回传 LLM
3. `GET /api/tools` → 列出所有已注册工具
