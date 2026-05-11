# Stage 19: 基础 RAG

## 做了什么

- 文档分块策略（`DocumentChunker`）
- Embedding 向量化（`EmbeddingClient` 接口 + OpenAI 实现 + Mock 实现）
- 向量相似搜索（`VectorStore` 内存实现）
- 检索结果注入 prompt（`RagPipeline`）

新增/修改的关键类：
- `DocumentChunker` — 文档分块器，按固定大小 + 重叠切分
- `EmbeddingClient` — Embedding 接口：`embed(text) → double[]`
- `OpenAiEmbeddingClient` — OpenAI text-embedding-3-small 实现
- `MockEmbeddingClient` — Mock 实现（用于测试）
- `VectorStore` — 内存向量存储，余弦相似度搜索
- `RagPipeline` — RAG 管线：index → retrieve → augment
- `Chunk` — 分块 record（content, sourceFile, startLine, endLine, embedding）

## 解决了什么问题

**核心问题**：Agent 的知识仅来自训练数据和对话上下文，无法访问项目本地文档。

**学习目标**：
- 文档分块策略（固定窗口 + 重叠）
- Embedding 向量化与余弦相似度搜索
- RAG 管线的基本架构：index → retrieve → augment

## 怎么做的

1. **分块**：`DocumentChunker.chunk(content, filePath)` 按固定字符数（如 1000）切分，相邻块重叠 200 字符，保留行号信息
2. **向量化**：`OpenAiEmbeddingClient.embed(text)` 调用 OpenAI embedding API 返回 1536 维向量
3. **向量存储**：`VectorStore` 用 `List<Chunk>` 内存存储，`search(queryVector, topK)` 计算余弦相似度返回 topK
4. **索引管线**：`RagPipeline.indexFile()` → 分块 → 逐块 embedding → 存入 VectorStore
5. **检索增强**：`RagPipeline.buildAugmentedPrompt()` 将检索到的 chunk 拼接为上下文前缀 + 用户问题
6. **目录索引**：`indexDirectory()` 支持 glob 模式批量索引

## 关键文件

- `src/main/java/com/agent/rag/DocumentChunker.java` — 文档分块
- `src/main/java/com/agent/rag/EmbeddingClient.java` — Embedding 接口
- `src/main/java/com/agent/rag/OpenAiEmbeddingClient.java` — OpenAI 实现
- `src/main/java/com/agent/rag/MockEmbeddingClient.java` — Mock 实现
- `src/main/java/com/agent/rag/VectorStore.java` — 向量存储
- `src/main/java/com/agent/rag/RagPipeline.java` — RAG 管线
- `src/main/java/com/agent/rag/Chunk.java` — 分块 record

## 验证方式

1. 索引 10 个文件 → `ragPipeline.indexDirectory("src", "*.java")`
2. 问相关问题 → 检索到正确 chunk
3. 对比有/无 RAG 增强的回答质量
