package com.agent.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 为每次 Web 请求生成唯一 correlationId 并放入 MDC
 * CLI 模式下每轮设置
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 优先使用请求头中的 correlationId，否则生成新的
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = generateId();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * 为 CLI 模式设置 correlationId
     */
    public static String setCorrelationId() {
        String id = generateId();
        MDC.put(MDC_KEY, id);
        return id;
    }

    /**
     * 清除 correlationId
     */
    public static void clearCorrelationId() {
        MDC.remove(MDC_KEY);
    }

    /**
     * 获取当前 correlationId
     */
    public static String getCurrentCorrelationId() {
        return MDC.get(MDC_KEY);
    }

    private static String generateId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
