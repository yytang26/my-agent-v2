package com.agent.llm.claude;

import com.agent.llm.LlmClient;
import com.agent.llm.exception.AuthenticationException;
import com.agent.llm.exception.LlmException;
import com.agent.llm.exception.OverloadedException;
import com.agent.llm.exception.RateLimitException;
import com.agent.llm.model.ChatMessage;
import com.agent.llm.model.ChatResponse;
import com.agent.llm.model.ModelConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "claude", matchIfMissing = true)
public class ClaudeClient implements LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeClient.class);
    private static final String PROVIDER = "claude";

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

        try {
            ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                    baseUrl + "/v1/messages",
                    request,
                    ChatResponse.class
            );

            ChatResponse body = response.getBody();
            if (body == null) {
                throw new LlmException("Empty response from Claude API", 0, PROVIDER, true);
            }

            return body;
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            String bodyText = e.getResponseBodyAsString();
            logger.error("Claude API client error: status={}, body={}", status, bodyText);
            if (status == 401) {
                throw new AuthenticationException("Claude API authentication failed: " + bodyText, status, PROVIDER);
            } else if (status == 429) {
                long retryAfter = parseRetryAfter(e);
                throw new RateLimitException("Claude API rate limited: " + bodyText, status, PROVIDER, retryAfter);
            } else {
                throw new LlmException("Claude API client error: " + bodyText, e, status, PROVIDER, false);
            }
        } catch (HttpServerErrorException e) {
            int status = e.getStatusCode().value();
            String bodyText = e.getResponseBodyAsString();
            logger.error("Claude API server error: status={}, body={}", status, bodyText);
            if (status == 529) {
                throw new OverloadedException("Claude API overloaded: " + bodyText, status, PROVIDER);
            } else {
                throw new LlmException("Claude API server error: " + bodyText, e, status, PROVIDER, true);
            }
        } catch (RestClientException e) {
            logger.error("Claude API request failed: {}", e.getMessage());
            throw new LlmException("Claude API request failed: " + e.getMessage(), e, 0, PROVIDER, true);
        }
    }

    private long parseRetryAfter(HttpClientErrorException e) {
        List<String> retryAfterHeaders = e.getResponseHeaders() != null
                ? e.getResponseHeaders().get("retry-after")
                : null;
        if (retryAfterHeaders != null && !retryAfterHeaders.isEmpty()) {
            try {
                return Long.parseLong(retryAfterHeaders.get(0)) * 1000L;
            } catch (NumberFormatException ignored) {
            }
        }
        List<String> retryAfterMsHeaders = e.getResponseHeaders() != null
                ? e.getResponseHeaders().get("retry-after-ms")
                : null;
        if (retryAfterMsHeaders != null && !retryAfterMsHeaders.isEmpty()) {
            try {
                return Long.parseLong(retryAfterMsHeaders.get(0));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }
}
