package com.agent.context;

import com.agent.llm.exception.ContextOverflowException;
import com.agent.memory.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ContextOverflowHandler {

    private static final Logger log = LoggerFactory.getLogger(ContextOverflowHandler.class);

    private final RecoveryStrategy recoveryStrategy;
    private final PreflightTokenCheck preflightTokenCheck;
    private final TokenEstimator tokenEstimator;

    public ContextOverflowHandler(RecoveryStrategy recoveryStrategy,
                                  PreflightTokenCheck preflightTokenCheck,
                                  TokenEstimator tokenEstimator) {
        this.recoveryStrategy = recoveryStrategy;
        this.preflightTokenCheck = preflightTokenCheck;
        this.tokenEstimator = tokenEstimator;
    }

    public List<Message> handleOverflow(List<Message> messages, Exception cause) {
        log.warn("[ContextOverflowHandler] 处理上下文溢出, cause={}", cause != null ? cause.getMessage() : "unknown");

        int currentTokens = tokenEstimator.estimate(messages);
        int maxTokens = extractMaxTokens(cause);

        log.info("[ContextOverflowHandler] 当前 {} tokens, 最大限制 {} tokens", currentTokens, maxTokens);

        List<Message> recovered = recoveryStrategy.recover(messages, maxTokens > 0 ? maxTokens : currentTokens);

        int after = tokenEstimator.estimate(recovered);
        log.info("[ContextOverflowHandler] 恢复完成: {} tokens -> {} tokens (消息 {} 条 -> {} 条)",
                currentTokens, after, messages.size(), recovered.size());

        return recovered;
    }

    public List<Message> ensureFit(List<Message> messages, int contextWindowSize) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        int currentTokens = tokenEstimator.estimate(messages);
        if (!preflightTokenCheck.willOverflow(messages, contextWindowSize)) {
            log.debug("[ContextOverflowHandler] 预检通过, {} tokens < {} limit", currentTokens, contextWindowSize);
            return new ArrayList<>(messages);
        }

        log.warn("[ContextOverflowHandler] 预检超限: {} tokens > {} limit, 启动恢复", currentTokens, contextWindowSize);
        List<Message> recovered = recoveryStrategy.recover(messages, contextWindowSize);
        int after = tokenEstimator.estimate(recovered);
        log.info("[ContextOverflowHandler] ensureFit 完成: {} tokens -> {} tokens", currentTokens, after);
        return recovered;
    }

    private int extractMaxTokens(Exception cause) {
        if (cause instanceof ContextOverflowException ex) {
            return ex.getMaxTokens();
        }
        return 0;
    }
}
