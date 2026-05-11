# Stage 01: 项目脚手架 & 裸调 Claude API

## 做了什么

- 搭建 Maven + Spring Boot 3.x 项目骨架
- 实现最简 Claude API 调用：硬编码 prompt → 打印 LLM 回复
- 配置 `application.yml` 管理 API key 和 base URL

新增/修改的关键类：
- `Application` — Spring Boot 入口，支持 CLI 和 Web 双模式启动
- `ClaudeClient`（后重构为 `llm.claude.ClaudeClient`）— 用 `RestTemplate` POST 到 `/v1/messages`
- `ChatMessage` / `ChatResponse` — 消息与响应 POJO

## 解决了什么问题

**核心问题**：从零开始，如何让 Java 程序与 Claude API 通信？

**学习目标**：
- 理解 Claude Messages API 的请求/响应格式（`model`, `messages`, `max_tokens`）
- 掌握 Spring Boot 项目的基本结构和启动流程
- 学会用 `RestTemplate` 发起 HTTP 请求并解析 JSON 响应

## 怎么做的

1. **Maven 项目初始化**：`pom.xml` 引入 `spring-boot-starter-web`、Jackson、Spring Boot 3.x parent
2. **Claude API 对接**：通过 `RestTemplate.postForEntity()` 向 `${claude.base-url}/v1/messages` 发送 POST 请求
3. **认证方式**：请求头设置 `x-api-key` 和 `anthropic-version: 2023-06-01`
4. **配置管理**：`application.yml` 中配置 `claude.api-key`、`claude.base-url`、`claude.model`

## 关键文件

- `pom.xml` — Maven 依赖定义（spring-boot-starter-web, jackson）
- `src/main/java/com/agent/Application.java` — Spring Boot 主入口
- `src/main/java/com/agent/llm/claude/ClaudeClient.java` — Claude API 原始调用
- `src/main/java/com/agent/llm/model/ChatMessage.java` — 消息 POJO
- `src/main/java/com/agent/llm/model/ChatResponse.java` — 响应 POJO
- `src/main/resources/application.yml` — API key 和模型配置

## 验证方式

```bash
mvn spring-boot:run
# 控制台打印 Claude 的回复文本即成功
```
