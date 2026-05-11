# Stage 10: 代码工具（Grep, Glob, Bash）

## 做了什么

- 实现编码助手必备的搜索与执行工具
- 安全的 Bash 执行（超时控制、输出截断）
- Java ProcessBuilder 进程管理封装

新增/修改的关键类：
- `GrepTool` — 正则搜索文件内容，返回匹配行
- `GlobTool` — 文件模式匹配（`**/*.java`），返回文件路径列表
- `BashTool` — Shell 命令执行（超时 + 输出截断）
- `ProcessRunner` — ProcessBuilder 封装，管理进程生命周期
- `OutputTruncator` — 大输出截断，防止超长结果占满上下文

## 解决了什么问题

**核心问题**：编码助手需要搜索代码、查找文件、执行命令，但直接执行 Shell 命令有安全和稳定性风险。

**学习目标**：
- 正则表达式搜索的实现
- glob 模式匹配的文件发现
- 进程超时控制和输出截断
- 安全的命令执行沙箱

## 怎么做的

1. **GrepTool**：遍历工作区文件，逐行 `Pattern.matcher()` 匹配，返回文件名 + 行号 + 匹配内容
2. **GlobTool**：用 `Files.walk()` + `PathMatcher("glob:...")` 递归匹配文件路径
3. **BashTool**：`ProcessRunner` 用 `ProcessBuilder` 启动子进程，`process.waitFor(timeout)` 限制执行时间
4. **超时控制**：超时后 `process.destroyForcibly()` 强制终止，返回超时提示
5. **输出截断**：`OutputTruncator` 检查 stdout/stderr 长度，超过阈值截断并添加 `[truncated]` 标记

## 关键文件

- `src/main/java/com/agent/tool/code/GrepTool.java` — 正则搜索
- `src/main/java/com/agent/tool/code/GlobTool.java` — 文件匹配
- `src/main/java/com/agent/tool/code/BashTool.java` — Shell 命令执行
- `src/main/java/com/agent/tool/code/ProcessRunner.java` — ProcessBuilder 封装
- `src/main/java/com/agent/tool/code/OutputTruncator.java` — 输出截断

## 验证方式

1. "找所有含 TODO 的 Java 文件" → GrepTool 返回匹配结果
2. `mvn --version` → BashTool 返回版本号
3. 超长输出的命令 → OutputTruncator 截断
