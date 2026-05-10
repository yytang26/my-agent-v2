package com.agent.web;

import com.agent.core.AgentResponse;
import com.agent.memory.Message;
import com.agent.memory.MessageRole;
import com.agent.tracking.TokenTracker;
import com.agent.tracking.UsageSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final WebSessionManager webSessionManager;
    private final WebAgentLoop webAgentLoop;
    private final TokenTracker tokenTracker;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatController(WebSessionManager webSessionManager,
                          WebAgentLoop webAgentLoop,
                          TokenTracker tokenTracker) {
        this.webSessionManager = webSessionManager;
        this.webAgentLoop = webAgentLoop;
        this.tokenTracker = tokenTracker;
    }

    @PostMapping("/api/sessions")
    public ResponseEntity<Map<String, String>> createSession() {
        WebSessionManager.WebSession session = webSessionManager.createSession();
        Map<String, String> result = new HashMap<>();
        result.put("sessionId", session.sessionId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/sessions")
    public ResponseEntity<List<WebSessionInfo>> listSessions() {
        return ResponseEntity.ok(webSessionManager.listSessions());
    }

    @DeleteMapping("/api/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable String id) {
        webSessionManager.deleteSession(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/chat")
    public SseEmitter chat(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId();
        String message = request.getMessage();

        WebSessionManager.WebSession session = webSessionManager.getSession(sessionId);
        if (session == null) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new IllegalArgumentException("会话不存在: " + sessionId));
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(300000L);

        executor.execute(() -> {
            try {
                AgentResponse response = webAgentLoop.run(message, session.memory(), emitter);
                log.info("[ChatController] 会话 {} 处理完成, 迭代: {}", sessionId, response.getTotalIterations());
                emitter.complete();
            } catch (Exception e) {
                log.error("[ChatController] 会话 {} 处理出错", sessionId, e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of("message", e.getMessage())));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @GetMapping("/api/sessions/{id}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable String id) {
        WebSessionManager.WebSession session = webSessionManager.getSession(id);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        List<MessageDto> result = new ArrayList<>();
        for (Message msg : session.memory().getMessages()) {
            if (msg.role() == MessageRole.SYSTEM) {
                continue;
            }
            result.add(new MessageDto(msg.role().name().toLowerCase(), msg.content(), msg.timestamp()));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/cost")
    public ResponseEntity<Map<String, Object>> getCost() {
        UsageSummary summary = tokenTracker.getSummary();
        Map<String, Object> result = new HashMap<>();
        result.put("totalInputTokens", summary.getTotalInputTokens());
        result.put("totalOutputTokens", summary.getTotalOutputTokens());
        result.put("totalTokens", summary.getTotalTokens());
        result.put("totalCostUsd", summary.getTotalCostUsd());
        result.put("requestCount", summary.getRequestCount());
        return ResponseEntity.ok(result);
    }
}
