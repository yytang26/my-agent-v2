# My Agent - AI 编程助手

一个基于 Java 构建的 AI Agent，类似 Claude Code / Qoder。

## 技术栈
- Java 21 + Spring Boot 3.x
- Claude API (Anthropic)
- JLine 3 (CLI 交互)
- Spring Data JPA + H2 (持久化)

## 快速开始

### Mock 模式（无需 API Key）
```
mvn spring-boot:run -Dspring-boot.run.profiles=mock
```

### 真实模式
```
CLAUDE_API_KEY=your-key mvn spring-boot:run
```

### 打包运行
```
mvn package -DskipTests
java -jar target/my-agent-1.0.0-SNAPSHOT.jar --spring.profiles.active=mock
```

## 功能
- CLI REPL 交互式终端
- Web GUI 聊天界面
- 文件系统工具 (读/写/编辑/列目录)
- 代码工具 (Grep/Glob/Bash)
- RAG 知识检索
- MCP 插件系统
- 子 Agent 委派
- 流式输出
- 权限系统
- 会话持久化
- 上下文压缩
- 安全沙箱
- 跨会话长期记忆
- 结构化日志 + 关联 ID
- 系统指标收集
- Agent 评估框架

## 课程阶段
本项目按 26 个阶段循序渐进构建，每个阶段对应一个 git 分支 (stage-01 ~ stage-26)。
