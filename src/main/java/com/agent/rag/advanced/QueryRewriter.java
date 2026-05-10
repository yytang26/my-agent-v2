package com.agent.rag.advanced;

import com.agent.llm.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QueryRewriter {

    private static final Logger log = LoggerFactory.getLogger(QueryRewriter.class);

    private final LlmClient llmClient;
    private final boolean mockMode;

    public QueryRewriter(LlmClient llmClient,
                         @Value("${llm.provider:mock}") String llmProvider) {
        this.llmClient = llmClient;
        this.mockMode = "mock".equalsIgnoreCase(llmProvider);
    }

    public String rewrite(String query) {
        if (query == null || query.isBlank()) {
            return query;
        }

        if (mockMode) {
            log.info("[QueryRewriter] Mock 模式，返回模拟假设文档");
            return "关于 " + query + " 的文档: " + query + " 是一个重要的主题，涉及多个方面的知识和应用场景。";
        }

        String prompt = "请写一段简短的文档片段，来回答以下问题: " + query
                + "\n\n要求:\n1. 文档片段应包含与问题相关的关键信息\n2. 长度在 100-200 字之间\n3. 用中文回答";

        try {
            String rewritten = llmClient.ask(prompt);
            log.info("[QueryRewriter] 查询改写完成: {} -> {}", query, rewritten.substring(0, Math.min(50, rewritten.length())) + "...");
            return rewritten;
        } catch (Exception e) {
            log.warn("[QueryRewriter] LLM 改写失败，使用原始查询: {}", e.getMessage());
            return query;
        }
    }
}
