# Stage 25: 安全、沙箱与 Human-in-the-Loop

## 做了什么

- Prompt 注入检测（`InjectionDetector`）
- 沙箱执行（`SandboxExecutor`，资源限制）
- 风险评分（`RiskScorer` + `RiskAssessment`）
- 置信度驱动的人工介入（`HumanEscalation`）
- 输出清洗（`OutputSanitizer`）
- 统一安全网关（`SecurityGateway`）

新增/修改的关键类：
- `SecurityGateway` — 安全网关，组合所有安全检查
- `InjectionDetector` — Prompt 注入检测器（关键词 + 模式匹配）
- `InjectionResult` — 注入检测结果
- `RiskScorer` — 风险评分器，根据工具类型和参数评估风险等级
- `RiskAssessment` — 风险评估结果（level, score, reason）
- `RiskLevel` — 枚举：LOW, MEDIUM, HIGH, CRITICAL
- `SandboxExecutor` — 沙箱执行器，限制 bash 命令的资源使用
- `SandboxConfig` — 沙箱配置（timeout, maxOutput, blockedCommands）
- `SandboxResult` — 沙箱执行结果
- `HumanEscalation` — 人工介入决策器
- `EscalationDecision` / `EscalationLevel` — 介入决策和级别
- `OutputSanitizer` — 输出清洗器，过滤敏感信息
- `SecurityCheckResult` — 安全检查综合结果

## 解决了什么问题

**核心问题**：Agent 执行不受控的代码和操作，存在 Prompt 注入、数据泄露、恶意命令等安全风险。

**学习目标**：
- Prompt 注入检测的常见模式
- 沙箱隔离执行
- 风险评分与分级
- Human-in-the-Loop 的决策逻辑

## 怎么做的

1. **安全网关**：`SecurityGateway.check(toolName, arguments, userInput)` 串联注入检测 → 风险评分 → 沙箱判断 → 人工介入
2. **注入检测**：`InjectionDetector.detect(userInput)` 匹配已知注入模式（"ignore previous instructions"、"system prompt" 等）
3. **风险评分**：`RiskScorer.assess(toolName, arguments)` 对 bash/write 操作赋予高风险，read 低风险
4. **沙箱执行**：`SandboxExecutor` 对 bash 命令限制超时（30s）、输出大小、禁止危险命令（rm -rf /）
5. **人工介入**：`HumanEscalation.shouldEscalate(risk, confidence)` 当风险高且置信度低时决定介入
6. **输出清洗**：`OutputSanitizer.sanitize()` 过滤 API key、密码等敏感信息模式

## 关键文件

- `src/main/java/com/agent/security/SecurityGateway.java` — 安全网关
- `src/main/java/com/agent/security/InjectionDetector.java` — 注入检测
- `src/main/java/com/agent/security/RiskScorer.java` — 风险评分
- `src/main/java/com/agent/security/SandboxExecutor.java` — 沙箱执行
- `src/main/java/com/agent/security/HumanEscalation.java` — 人工介入
- `src/main/java/com/agent/security/OutputSanitizer.java` — 输出清洗

## 验证方式

1. 输入 "ignore previous instructions" → 注入检测拦截
2. `rm -rf /` → 沙箱阻止
3. 高风险操作 → 触发人工介入确认
