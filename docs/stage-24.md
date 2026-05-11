# Stage 24: 高级任务规划

## 做了什么

- LLM 驱动的任务分解为 DAG（有向无环图）
- 子任务状态机（`TaskState`）
- 并行子任务执行（`PlanTaskExecutor`）
- 失败后重规划（`Replanner`）

新增/修改的关键类：
- `TaskPlanner` — 任务规划器，将复杂任务分解为子任务 DAG
- `TaskDAG` — 任务有向无环图，维护依赖关系，验证无环
- `PlanTask` — 子任务 record（id, description, dependsOn, state）
- `PlanTaskExecutor` — 任务执行器，按拓扑排序执行，支持并行
- `TaskState` — 枚举：PENDING, RUNNING, COMPLETED, FAILED
- `Replanner` — 重规划器，失败时调整后续计划

## 解决了什么问题

**核心问题**：复杂任务需要多步骤协调，单次 Agent Loop 难以高效完成。

**学习目标**：
- DAG 任务分解与依赖管理
- 拓扑排序与并行执行
- 失败后的自适应重规划

## 怎么做的

1. **任务分解**：`TaskPlanner.plan(complexTask)` 构建提示词让 LLM 输出 JSON 格式的子任务列表（含 id、description、dependsOn）
2. **DAG 构建**：`TaskDAG.addTask(PlanTask)` 添加子任务，`validateNoCycle()` 用 DFS 验证无环
3. **LLM 回退**：LLM 分解失败时回退到 `planMock()` 模式（简单三步分解）
4. **JSON 解析**：手动正则解析 LLM 的 JSON 输出，避免 Jackson 依赖复杂性
5. **执行策略**：`PlanTaskExecutor` 按拓扑排序执行，无依赖的子任务并行执行
6. **重规划**：`Replanner` 在子任务失败时重新调整后续任务（修改依赖、添加新任务）

## 关键文件

- `src/main/java/com/agent/planner/TaskPlanner.java` — 任务规划器
- `src/main/java/com/agent/planner/TaskDAG.java` — 任务 DAG
- `src/main/java/com/agent/planner/PlanTask.java` — 子任务
- `src/main/java/com/agent/planner/PlanTaskExecutor.java` — 执行器
- `src/main/java/com/agent/planner/TaskState.java` — 状态枚举
- `src/main/java/com/agent/planner/Replanner.java` — 重规划器

## 验证方式

1. 复杂任务 → 分解为多个子任务 → DAG 可视化
2. 独立子任务并行执行 → 总时间缩短
3. 某子任务失败 → 重规划 → 继续执行
