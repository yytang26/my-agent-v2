# Stage 26: 生产工程化 & 毕业项目

## 做了什么

- 数据库持久化（Spring Data JPA + H2）
- 长期记忆（`LongTermMemory`，跨会话实体提取）
- 可观测性（`MetricsCollector`，结构化日志 + 关联 ID）
- 评估框架（`AgentEvaluator` + `EvalTestCase`）
- `PersistentSessionManager` 基于 JPA 的持久化
- 打包为可分发 JAR

新增/修改的关键类：
- `SessionRepository` — JPA Repository，Session 实体的 CRUD
- `SessionEntity` — JPA 实体，映射 sessions 表
- `PersistentSessionManager` — 基于 JPA 的持久化会话管理器
- `LongTermMemory` — 长期记忆，跨会话知识提取和检索
- `MemoryEntry` / `MemoryEntryRepository` — 记忆条目实体和 Repository
- `MetricsCollector` — 系统指标收集器（请求数、token、费用、响应时间、错误率）
- `CorrelationIdFilter` — 关联 ID 过滤器，为每个请求分配唯一 trace ID
- `AgentEvaluator` — 评估器，运行测试用例并生成报告
- `EvalTestCase` / `EvalResult` / `EvalReport` — 评估用例、结果、报告

## 解决了什么问题

**核心问题**：Agent 功能完整但不够健壮——数据可能丢失、无法度量运行质量、缺少自动化测试。

**学习目标**：
- Spring Data JPA 持久化
- 长期记忆的跨会话管理
- 可观测性三支柱（日志、指标、追踪）
- 评估驱动的质量保障

## 怎么做的

1. **JPA 持久化**：`SessionEntity` 映射到 H2 数据库，`SessionRepository` 提供标准 CRUD + `findAllByOrderByUpdatedAtDesc()`
2. **消息序列化**：`PersistentSessionManager` 将 `List<Message>` 序列化为 JSON 字符串存储在 `messagesJson` 列
3. **长期记忆**：`LongTermMemory` 从对话中提取关键实体和知识，存入 `MemoryEntry` 表，跨会话检索复用
4. **指标收集**：`MetricsCollector` 用 `AtomicLong` 收集请求数、token、费用、响应时间、工具调用、错误率
5. **关联 ID**：`CorrelationIdFilter` 在 MDC 中设置 `correlationId`，所有日志自动携带
6. **评估框架**：`AgentEvaluator` 从 `eval/test-cases.json` 加载测试用例 → 运行 AgentLoop → 检查工具调用和输出
7. **可执行 JAR**：`pom.xml` 配置 `spring-boot-maven-plugin` → `mvn package` 生成可执行 JAR

## 关键文件

- `src/main/java/com/agent/persistence/SessionRepository.java` — JPA Repository
- `src/main/java/com/agent/persistence/SessionEntity.java` — JPA 实体
- `src/main/java/com/agent/persistence/PersistentSessionManager.java` — JPA 持久化管理
- `src/main/java/com/agent/persistence/LongTermMemory.java` — 长期记忆
- `src/main/java/com/agent/persistence/MemoryEntryRepository.java` — 记忆 Repository
- `src/main/java/com/agent/observability/MetricsCollector.java` — 指标收集
- `src/main/java/com/agent/observability/CorrelationIdFilter.java` — 关联 ID
- `src/main/java/com/agent/eval/AgentEvaluator.java` — 评估器
- `src/main/resources/eval/test-cases.json` — 评估测试用例

## 验证方式

1. 杀进程重启 → session 不丢失（JPA 持久化）
2. 评估套件 → 报告成功率
3. `mvn package` → `java -jar target/my-agent-*.jar` → 完整启动
4. `/cost` 和 MetricsCollector 显示运行指标
