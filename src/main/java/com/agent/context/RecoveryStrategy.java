package com.agent.context;

import com.agent.memory.Message;
import com.agent.memory.MessageRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RecoveryStrategy {

    private static final Logger log = LoggerFactory.getLogger(RecoveryStrategy.class);

    private final ContextCompressor contextCompressor;
    private final AggressiveCompressor aggressiveCompressor;
    private final PreflightTokenCheck preflightTokenCheck;
    private final TokenEstimator tokenEstimator;

    public RecoveryStrategy(ContextCompressor contextCompressor,
                            AggressiveCompressor aggressiveCompressor,
                            PreflightTokenCheck preflightTokenCheck,
                            TokenEstimator tokenEstimator) {
        this.contextCompressor = contextCompressor;
        this.aggressiveCompressor = aggressiveCompressor;
        this.preflightTokenCheck = preflightTokenCheck;
        this.tokenEstimator = tokenEstimator;
    }

    public List<Message> recover(List<Message> messages, int contextWindowSize) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        int before = tokenEstimator.estimate(messages);
        log.info("[Recovery] 开始恢复, 当前 {} tokens, 限制 {} tokens", before, contextWindowSize);

        // 1. 先尝试正常 ContextCompressor（分层压缩）
        List<Message> result = contextCompressor.compressIfNeeded(messages);
        int afterNormal = tokenEstimator.estimate(result);
        log.info("[Recovery] 分层压缩后: {} tokens -> {} tokens", before, afterNormal);

        if (!preflightTokenCheck.willOverflow(result, contextWindowSize)) {
            log.info("[Recovery] 分层压缩成功, 无需进一步恢复");
            return result;
        }

        // 2. 如果仍然超限 -> 使用 AggressiveCompressor
        log.warn("[Recovery] 分层压缩后仍超限, 使用激进压缩");
        result = aggressiveCompressor.compress(result, contextWindowSize);
        int afterAggressive = tokenEstimator.estimate(result);
        log.info("[Recovery] 激进压缩后: {} tokens -> {} tokens", afterNormal, afterAggressive);

        if (!preflightTokenCheck.willOverflow(result, contextWindowSize)) {
            log.info("[Recovery] 激进压缩成功");
            return result;
        }

        // 3. 如果仍超限 -> 只保留系统提示词 + 最后一条用户消息
        log.error("[Recovery] 激进压缩后仍超限, 执行最极端压缩: 仅保留系统提示词 + 最后一条用户消息");
        result = extremeCompress(result);
        int afterExtreme = tokenEstimator.estimate(result);
        log.info("[Recovery] 极端压缩后: {} tokens -> {} tokens", afterAggressive, afterExtreme);

        return result;
    }

    private List<Message> extremeCompress(List<Message> messages) {
        List<Message> result = new ArrayList<>();
        Message systemMsg = null;
        Message lastUserMsg = null;

        for (Message msg : messages) {
            if (msg.role() == MessageRole.SYSTEM) {
                systemMsg = msg;
            }
        }

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg.role() == MessageRole.USER && lastUserMsg == null) {
                lastUserMsg = msg;
                break;
            }
        }

        if (systemMsg != null) {
            result.add(systemMsg);
        }
        if (lastUserMsg != null) {
            result.add(lastUserMsg);
        }

        if (result.isEmpty() && !messages.isEmpty()) {
            result.add(messages.get(messages.size() - 1));
        }

        return result;
    }
}
