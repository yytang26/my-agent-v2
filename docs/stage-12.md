# Stage 12: 权限系统

## 做了什么

- 实现分层权限模型（ALLOW / ASK / DENY per tool）
- 交互式确认："Agent 想写入 src/Main.java — 允许? [y/n/always]"
- Session 级 "always allow" 白名单机制
- 默认策略：读操作 ALLOW，写操作 ASK，bash ASK

新增/修改的关键类：
- `PermissionManager` — 权限管理器，维护默认规则 + session 白名单
- `PermissionRule` — 单条权限规则
- `PermissionPolicy` — 枚举：ALLOW, ASK, DENY
- `PermissionResponse` — 枚举：YES, NO, ALWAYS, DENY_FOREVER
- `InteractivePrompter` — 交互式确认提示
- `SessionAllowList` — Session 级白名单

## 解决了什么问题

**核心问题**：Agent 自主执行写文件、运行命令等危险操作，可能导致不可逆的损害。

**学习目标**：
- 分层权限模型的设计（自动允许 / 需确认 / 拒绝）
- Session 级白名单避免重复确认
- 人机交互确认流程

## 怎么做的

1. **默认规则**：`@PostConstruct` 初始化默认策略——`read_file`/`list_directory`/`grep`/`glob` → ALLOW，`write_file`/`edit_file`/`bash` → ASK
2. **检查流程**：`checkPermission(toolName, arguments)` → 先查 Session 白名单 → 再查默认规则 → ASK 则交互确认
3. **交互确认**：`InteractivePrompter.prompt()` 在终端显示 "Agent wants to use [tool] — Allow? [y/n/always/deny-forever]"
4. **Always 白名单**：用户回答 "always" → `sessionAllowList.add(toolName)` → 后续同工具不再询问
5. **DENY_FOREVER**：用户选择永久拒绝 → `defaultRules.put(toolName, DENY)` → 后续自动拒绝
6. **ToolExecutor 集成**：`ToolExecutor.execute()` 执行前先调用 `PermissionManager.checkPermission()`

## 关键文件

- `src/main/java/com/agent/permission/PermissionManager.java` — 权限管理器
- `src/main/java/com/agent/permission/PermissionRule.java` — 权限规则
- `src/main/java/com/agent/permission/PermissionPolicy.java` — 策略枚举
- `src/main/java/com/agent/permission/PermissionResponse.java` — 用户响应枚举
- `src/main/java/com/agent/permission/InteractivePrompter.java` — 交互确认
- `src/main/java/com/agent/permission/SessionAllowList.java` — Session 白名单

## 验证方式

1. Agent 尝试写文件 → 弹出确认提示
2. 回答 "always" → 后续写操作不再询问
3. 回答 "n" → 工具执行被拒绝
