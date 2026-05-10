package com.agent.llm.claude;

import com.agent.llm.LlmClient;
import com.agent.llm.exception.AuthenticationException;
import com.agent.llm.exception.LlmException;
import com.agent.llm.exception.OverloadedException;
import com.agent.llm.exception.RateLimitException;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ModelConfig;
import com.agent.llm.model.StreamChunk;
import com.agent.tool.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("!mock")
@Primary
public class StreamingClaudeClient implements LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(StreamingClaudeClient.class);
    private static final String PROVIDER = "claude";

    @Value("${claude.api-key}")
    private String apiKey;

    @Value("${claude.base-url}")
    private String baseUrl;

    @Value("${claude.model}")
    private String model;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ClaudeClient claudeClient;

    public StreamingClaudeClient(WebClient.Builder webClientBuilder, ClaudeClient claudeClient) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
        this.claudeClient = claudeClient;
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages, ModelConfig config) {
        return claudeClient.chat(messages, config);
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages, ModelConfig config, List<ToolDefinition> tools) {
        return claudeClient.chat(messages, config, tools);
    }

    @Override
    public Flux<StreamChunk> chatStream(List<ChatMessage> messages, ModelConfig config, List<ToolDefinition> tools) {
        Map<String, Object> requestBody = buildRequestBody(messages, config, tools);

        return webClient.post()
                .uri(baseUrl + "/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(mapError(response.statusCode().value(), body)))
                )
                .bodyToFlux(String.class)
                .flatMap(line -> parseSseLine(line).map(Flux::just).orElseGet(Flux::empty))
                .onErrorResume(e -> {
                    logger.error("Streaming error, falling back gracefully", e);
                    return Flux.empty();
                });
    }

    private Map<String, Object> buildRequestBody(List<ChatMessage> messages, ModelConfig config, List<ToolDefinition> tools) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getName() != null ? config.getName() : model);
        requestBody.put("max_tokens", config.getMaxTokens());
        requestBody.put("messages", messages);
        requestBody.put("stream", true);
        if (tools != null && !tools.isEmpty()) {
            requestBody.put("tools", tools.stream().map(ToolDefinition::toMap).toList());
        }
        return requestBody;
    }

    private java.util.Optional<StreamChunk> parseSseLine(String line) {
        if (!line.startsWith("data: ")) {
            return java.util.Optional.empty();
        }
        String json = line.substring(6);
        if ("[DONE]".equals(json.trim())) {
            return java.util.Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            String type = root.has("type") ? root.get("type").asText() : "";
            return switch (type) {
                case "content_block_start" -> parseContentBlockStart(root);
                case "content_block_delta" -> parseContentBlockDelta(root);
                case "content_block_stop" -> parseContentBlockStop(root);
                case "message_start" -> java.util.Optional.of(new StreamChunk(com.agent.llm.model.StreamEventType.MESSAGE_START));
                case "message_delta" -> java.util.Optional.of(new StreamChunk(com.agent.llm.model.StreamEventType.MESSAGE_DELTA));
                case "message_stop" -> java.util.Optional.of(new StreamChunk(com.agent.llm.model.StreamEventType.MESSAGE_STOP));
                default -> java.util.Optional.empty();
            };
        } catch (Exception e) {
            logger.warn("Failed to parse SSE line: {}", line, e);
            return java.util.Optional.empty();
        }
    }

    private java.util.Optional<StreamChunk> parseContentBlockStart(JsonNode root) {
        JsonNode block = root.get("content_block");
        if (block == null) {
            return java.util.Optional.empty();
        }
        String blockType = block.has("type") ? block.get("type").asText() : "text";
        StreamChunk chunk = new StreamChunk(com.agent.llm.model.StreamEventType.CONTENT_BLOCK_START);
        if ("tool_use".equals(blockType)) {
            chunk.setToolUseId(block.has("id") ? block.get("id").asText() : null);
            chunk.setToolName(block.has("name") ? block.get("name").asText() : null);
        } else if (block.has("text")) {
            chunk.setText(block.get("text").asText());
        }
        chunk.setIndex(root.has("index") ? root.get("index").asInt() : 0);
        return java.util.Optional.of(chunk);
    }

    private java.util.Optional<StreamChunk> parseContentBlockDelta(JsonNode root) {
        JsonNode delta = root.get("delta");
        if (delta == null) {
            return java.util.Optional.empty();
        }
        String deltaType = delta.has("type") ? delta.get("type").asText() : "text_delta";
        StreamChunk chunk = new StreamChunk(com.agent.llm.model.StreamEventType.CONTENT_BLOCK_DELTA);
        if ("text_delta".equals(deltaType) && delta.has("text")) {
            chunk.setText(delta.get("text").asText());
        } else if ("input_json_delta".equals(deltaType) && delta.has("partial_json")) {
            chunk.setText(delta.get("partial_json").asText());
        }
        chunk.setIndex(root.has("index") ? root.get("index").asInt() : 0);
        return java.util.Optional.of(chunk);
    }

    private java.util.Optional<StreamChunk> parseContentBlockStop(JsonNode root) {
        StreamChunk chunk = new StreamChunk(com.agent.llm.model.StreamEventType.CONTENT_BLOCK_STOP);
        chunk.setIndex(root.has("index") ? root.get("index").asInt() : 0);
        return java.util.Optional.of(chunk);
    }

    private Throwable mapError(int status, String body) {
        if (status == 401) {
            return new AuthenticationException("Claude API authentication failed: " + body, status, PROVIDER);
        } else if (status == 429) {
            return new RateLimitException("Claude API rate limited: " + body, status, PROVIDER, 0);
        } else if (status == 529) {
            return new OverloadedException("Claude API overloaded: " + body, status, PROVIDER);
        } else if (status >= 500) {
            return new LlmException("Claude API server error: " + body, null, status, PROVIDER, true);
        } else {
            return new LlmException("Claude API client error: " + body, null, status, PROVIDER, false);
        }
    }
}
