package com.agent.llm;

import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
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
@Profile("!mock")
public class ClaudeRawClient implements LlmClient {

    @Value("${claude.api-key}")
    private String apiKey;

    @Value("${claude.base-url}")
    private String baseUrl;

    @Value("${claude.model}")
    private String model;

    @Value("${claude.max-tokens}")
    private int maxTokens;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String ask(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("messages", List.of(new ChatMessage("user", prompt)));

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

        String text = body.getFirstTextContent();
        if (text == null) {
            throw new RuntimeException("No text content in Claude response");
        }

        return text;
    }
}
