# Stage 03: 分层配置管理

## 做了什么

- 实现类似 git config 的多层级配置系统：GLOBAL → USER → PROJECT → CLI → SESSION
- 配置项按优先级逐层覆盖，高优先级覆盖低优先级
- 运行时可通过 Session 级覆盖动态修改配置
- 类型安全的配置访问（`AgentConfig` POJO）

新增/修改的关键类：
- `ConfigManager` — 核心管理器，加载并合并多层配置
- `ConfigSource` — 枚举：GLOBAL, USER, PROJECT, CLI, SESSION
- `AgentConfig` — 类型化配置 POJO，支持 `merge()` 合并
- `ConfigLoader` — YAML 文件 I/O + 解析

## 解决了什么问题

**核心问题**：所有配置硬编码在 `application.yml` 中，无法按项目/用户/会话灵活覆盖。

**学习目标**：
- 理解分层配置的必要性和优先级规则
- 掌握 `AgentConfig.merge()` 的非空覆盖模式
- 学会 Spring `Environment` 与自定义配置的集成

## 怎么做的

1. **四层加载**：`@PostConstruct` 时依次加载 GLOBAL（`~/.my-agent/config.yml`）、PROJECT（`./.my-agent/config.yml`）、CLI（Spring Environment）、SESSION（运行时覆盖）
2. **合并策略**：`AgentConfig.merge()` 遍历所有字段，仅当上层字段非 null 时覆盖下层
3. **Session 覆盖**：`setSessionOverride(key, value)` 支持运行时修改配置（如切换模型、API key）
4. **YAML 解析**：`ConfigLoader` 用 Jackson `YAMLFactory` 解析配置文件，文件不存在时返回空 `AgentConfig`

## 关键文件

- `src/main/java/com/agent/config/ConfigManager.java` — 合并分层配置
- `src/main/java/com/agent/config/ConfigSource.java` — 优先级枚举
- `src/main/java/com/agent/config/AgentConfig.java` — 类型化配置 POJO + merge()
- `src/main/java/com/agent/config/ConfigLoader.java` — YAML 文件加载

## 验证方式

1. 在项目目录创建 `.my-agent/config.yml`，设置 `model: claude-3-haiku`
2. 全局 `~/.my-agent/config.yml` 设置 `model: claude-3-sonnet`
3. 启动后项目级配置覆盖全局 → 最终 model 为 `claude-3-haiku`
4. CLI 参数 `--claude.model=claude-3-opus` → 最终为 `claude-3-opus`
