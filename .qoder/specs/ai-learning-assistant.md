# AI 学习助手 (Study Helper) 实现方案

## Context

用户希望构建一个Web应用，帮助学生和职场人士学习。核心场景包括：
- 学生输入数学题获取完整解题过程
- 儿童学习古诗词（配历史故事，生动有趣）
- 职场人精炼长文章（公众号文章摘要化）

应用需支持"儿童模式"和"成人模式"切换，兼顾两类人群的视觉体验。

## 技术栈

| 层 | 选择 |
|---|---|
| 后端 | Java 21 + Spring Boot 3.3.x + Maven |
| 数据库 | H2（内嵌，零配置） |
| ORM | Spring Data JPA |
| 前端 | React + Vite + TypeScript |
| 样式 | Tailwind CSS |
| 状态管理 | Zustand |
| HTTP客户端 | Axios |
| AI | Mock数据（Profile隔离，后续可替换真实AI） |

## 6个功能模块

1. **个性化AI导师** - 输入数学题，展示分步解题过程和思路
2. **智能对话指导** - 苏格拉底式引导对话，多轮交互
3. **知识探索** - 古诗词学习 + 历史故事 + 生动讲解
4. **内容精炼** - 长文章 → 简洁有趣的摘要 + 关键点
5. **里程碑助手** - 目标设定、进度追踪、成就系统
6. **学习关怀** - 每日鼓励、学习贴士、心情记录

## 项目结构

```
study-helper/
├── study-helper-server/          # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/study/
│       ├── StudyHelperApplication.java
│       ├── common/               # 统一响应R<T>、CORS、异常处理
│       ├── ai/                   # AI服务抽象层
│       │   ├── AiService.java          # 接口
│       │   └── mock/MockAiService.java  # Mock实现(@Profile("mock"))
│       ├── module/
│       │   ├── tutor/            # AI导师
│       │   ├── chat/             # 智能对话
│       │   ├── explore/          # 知识探索
│       │   ├── digest/           # 内容精炼
│       │   ├── milestone/        # 里程碑
│       │   └── care/             # 学习关怀
│       └── user/                 # 用户配置
│
└── study-helper-web/             # React 前端
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── api/                  # API调用层
        ├── store/                # Zustand状态(模式切换、对话等)
        ├── components/           # 共享组件(Layout、Chat、Card)
        ├── pages/                # 路由页面(Home、Tutor、Chat、Explore、Digest、Milestone、Care)
        └── hooks/                # 自定义Hooks(useTheme、useMode)
```

## 关键API设计

基础路径: `/api/v1`，统一响应: `{ code: 0, msg: "success", data: T }`

| 模块 | 核心端点 | 说明 |
|------|----------|------|
| 导师 | POST `/tutor/solve` | 提交题目 → 分步解答 |
| 对话 | POST `/chat/sessions/{id}/messages` | 发送消息 → AI回复+引导问题 |
| 探索 | POST `/explore/poems/{id}/explain` | 诗词 → 故事化讲解 |
| 精炼 | POST `/digest/summarize` | 文章 → 摘要+关键点 |
| 里程碑 | CRUD `/milestones/goals` | 目标管理 |
| 关怀 | GET `/care/encouragement` | 获取鼓励语 |

## Mock AI 策略

```java
public interface AiService {
    SolveResponse generateSolution(String question, String subject);
    ChatReply generateChatReply(List<ChatMessage> history, String mode);
    Explanation generateExplanation(Long poemId, String style, String targetAge);
    Summary generateSummary(String content, String style);
    String generateEncouragement();
}
```

- `@Profile("mock")` 注解隔离Mock实现
- 根据输入关键字匹配预设JSON模板回复
- 可配置延迟(500-1500ms)模拟网络
- 后续替换只需新增`@Profile("prod")`实现类

## 模式切换设计

- 全局状态 `mode: 'child' | 'adult'` 存于 Zustand
- `<body data-mode="child">` 通过CSS变量切换主题
- 成人模式: 靛蓝主色(#4F46E5)，简洁，正常字号
- 儿童模式: 暖橙主色(#F59E0B)，大圆角，大字号，配微动画

## 开发阶段

### Stage-01: 项目骨架 + 首页

**后端:**
- 初始化 Spring Boot 项目(pom.xml、配置文件、启动类)
- 统一响应封装 `R<T>`、CORS配置、全局异常处理
- 健康检查端点 `/api/v1/health`
- AiService 接口定义

**前端:**
- 初始化 Vite + React + TS 项目
- 配置 Tailwind CSS、React Router、Zustand、Axios
- 主布局组件(侧边栏 + Header + 内容区)
- 首页: 6个功能卡片入口
- 模式切换组件(儿童/成人)

**验证:** 前后端可独立启动，首页展示6个功能卡片，模式切换有视觉变化

---

### Stage-02: AI导师 + 智能对话

**后端:**
- TutorController/Service: 解题接口
- ChatController/Service: 对话接口 + 会话管理
- ChatSession/ChatMessage JPA实体
- MockAiService: 数学解题模板 + 苏格拉底对话逻辑

**前端:**
- Tutor页面: 输入题目 → 展示分步解答(Markdown渲染)
- Chat页面: 对话气泡界面 + 输入框 + 历史会话

**验证:** 输入数学题看到分步解答；对话可多轮交互，AI有引导性回复

---

### Stage-03: 知识探索 + 内容精炼

**后端:**
- ExploreController/Service + 预置古诗词数据(JSON)
- DigestController/Service + 文章摘要Mock
- MockAiService 补充诗词讲解 + 摘要逻辑

**前端:**
- Explore页面: 诗词列表(卡片) → 详情 → AI故事化讲解
- Digest页面: 输入文章 → 展示摘要 + 关键点
- 儿童模式特殊处理(大字、注音提示)

**验证:** 浏览诗词并查看生动讲解；粘贴文章获得精炼摘要

---

### Stage-04: 里程碑 + 学习关怀

**后端:**
- Goal/Achievement JPA实体 + CRUD接口
- CareController/Service + 预置鼓励语库

**前端:**
- Milestone页面: 目标创建/编辑、进度条、成就徽章
- Care页面: 每日鼓励卡片、学习贴士、心情记录
- 进度动画(Framer Motion)

**验证:** 创建目标并更新进度；查看随机鼓励语和学习贴士

---

### Stage-05: 打磨整合

- 两种模式视觉全面适配
- 页面切换动画
- 响应式(移动端基本可用)
- Loading/空状态/错误状态完善
- 首页聚合学习概览数据

**验证:** 全流程体验流畅，两种模式切换无异常，移动端可用

## 关键文件清单

| 文件 | 作用 |
|------|------|
| `study-helper-server/pom.xml` | 后端依赖管理 |
| `study-helper-server/src/.../ai/AiService.java` | AI核心接口 |
| `study-helper-server/src/.../ai/mock/MockAiService.java` | Mock实现 |
| `study-helper-server/src/.../common/result/R.java` | 统一响应 |
| `study-helper-web/package.json` | 前端依赖 |
| `study-helper-web/src/store/useAppStore.ts` | 全局状态(模式切换) |
| `study-helper-web/src/components/Layout/AppLayout.tsx` | 主布局 |
| `study-helper-web/src/pages/Home/index.tsx` | 首页 |

## 验证方式

1. 后端: `cd study-helper-server && mvn spring-boot:run -Dspring-boot.run.profiles=mock`
2. 前端: `cd study-helper-web && npm run dev`
3. 每个Stage完成后验证对应功能可正常使用
4. 浏览器访问前端页面，确认UI渲染和API调用正常
