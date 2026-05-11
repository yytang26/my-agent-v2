# Stage 09: 文件系统工具

## 做了什么

- 实现编码 Agent 的核心文件操作工具集
- 路径安全校验（`PathValidator` 防目录遍历攻击）
- 大文件和二进制文件处理

新增/修改的关键类：
- `ReadFileTool` — 读文件（支持行范围参数）
- `WriteFileTool` — 写/创建文件
- `EditFileTool` — 行级文本替换（old_text → new_text）
- `ListDirectoryTool` — 目录列表
- `PathValidator` — 所有操作限制在工作区根目录内

## 解决了什么问题

**核心问题**：编码助手必须能读写项目文件，但直接操作文件系统有安全风险。

**学习目标**：
- 实现安全的文件操作工具（防止路径遍历）
- 理解行级编辑 vs 全文替换的差异
- 掌握大文件读取的分页策略

## 怎么做的

1. **PathValidator**：所有文件操作先调用 `validate(path)` → `toAbsolutePath().normalize()` → 检查是否以 `workDir` 开头，拒绝 `../` 逃逸
2. **ReadFileTool**：用 `Files.readString()` 读取，支持 `startLine` / `endLine` 参数分页
3. **WriteFileTool**：用 `Files.writeString()` 写入，自动创建父目录
4. **EditFileTool**：读取全文 → `replace(oldText, newText)` → 写回，要求 `oldText` 唯一匹配
5. **ListDirectoryTool**：`Files.list()` 遍历目录，返回文件名 + 类型标识

## 关键文件

- `src/main/java/com/agent/tool/fs/ReadFileTool.java` — 读文件工具
- `src/main/java/com/agent/tool/fs/WriteFileTool.java` — 写文件工具
- `src/main/java/com/agent/tool/fs/EditFileTool.java` — 编辑文件工具
- `src/main/java/com/agent/tool/fs/ListDirectoryTool.java` — 目录列表工具
- `src/main/java/com/agent/tool/fs/PathValidator.java` — 路径安全校验

## 验证方式

1. "读 pom.xml" → 返回文件内容
2. "创建 hello.txt 内容为 Hello" → 文件生成
3. 尝试读取 `/etc/passwd` → PathValidator 阻止，返回安全错误
