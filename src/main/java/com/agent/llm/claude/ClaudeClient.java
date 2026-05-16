package com.agent.llm.claude;

import com.agent.llm.LlmClient;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ModelConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "claude", matchIfMissing = true)
public class ClaudeClient implements LlmClient {

    @Value("${claude.api-key}")
    private String apiKey;

    @Value("${claude.base-url}")
    private String baseUrl;

    @Value("${claude.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ChatResponse chat(List<ChatMessage> messages, ModelConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getName() != null ? config.getName() : model);
        requestBody.put("max_tokens", config.getMaxTokens());
        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                baseUrl + "/v1/messages",
                request,
                ChatResponse.class
        );

        ChatResponse body = response.getBody();
        if (body == null) {
            throw new RuntimeException("Empty response from Claude API");
        }

        return body;
    }
}
