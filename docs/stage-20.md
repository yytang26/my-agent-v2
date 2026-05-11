# Stage 20: 高级 RAG（多路召回、RRF、Rerank）

## 做了什么

- BM25 关键词检索（`BM25Retriever`）
- RRF (Reciprocal Rank Fusion) 融合（`RRFMerger`）
- LLM Reranking（`LlmReranker`）
- HyDE 查询改写（`QueryRewriter`）
- 组合为 `AdvancedRagPipeline`

新增/修改的关键类：
- `BM25Retriever` — BM25 关键词检索，基于词频和文档频率
- `RRFMerger` — 倒数排名融合，合并多路检索结果
- `LlmReranker` — LLM 重排，让 LLM 判断相关性并排序
- `QueryRewriter` — HyDE 查询改写，生成假设性文档改善检索
- `AdvancedRagPipeline` — 高级 RAG 管线，组合所有策略
- `ScoredChunk` — 带分数的分块 record

## 解决了什么问题

**核心问题**：基础 RAG 仅用向量相似度，对关键词精确匹配和语义理解不够。

**学习目标**：
- 多路召回（向量 + BM25）的优势
- RRF 融合算法
- LLM Reranking 提升相关性
- HyDE 查询改写改善召回

## 怎么做的

1. **BM25 检索**：`BM25Retriever` 基于 TF-IDF 变体，对关键词精确匹配更友好
2. **双路召回**：`AdvancedRagPipeline` 同时调用向量搜索和 BM25，获取两路候选
3. **RRF 融合**：`RRFMerger.merge(vectorResults, bm25Results)` 用 `1/(k + rank)` 公式合并排名，k=60
4. **LLM Reranking**：`LlmReranker.rerank(query, chunks)` 让 LLM 对候选 chunk 打分排序
5. **HyDE 改写**：`QueryRewriter.rewrite(query)` 先让 LLM 生成假设性回答，用回答做检索
6. **可配置管线**：`AdvancedRagPipeline` 支持开关各策略（enableReranking, enableQueryRewrite）

## 关键文件

- `src/main/java/com/agent/rag/advanced/BM25Retriever.java` — BM25 检索
- `src/main/java/com/agent/rag/advanced/RRFMerger.java` — RRF 融合
- `src/main/java/com/agent/rag/advanced/LlmReranker.java` — LLM 重排
- `src/main/java/com/agent/rag/advanced/QueryRewriter.java` — 查询改写
- `src/main/java/com/agent/rag/advanced/AdvancedRagPipeline.java` — 高级管线
- `src/main/java/com/agent/rag/advanced/ScoredChunk.java` — 带分数分块

## 验证方式

1. 对比基础 RAG vs 高级 RAG 的检索结果相关性
2. 开启/关闭 Reranking → 观察排序变化
3. HyDE 改写后检索更精准
