package com.agent.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpTransport implements McpTransport {

    private static final Logger log = LoggerFactory.getLogger(HttpTransport.class);

    private final McpServerConfig config;
    private final HttpClient httpClient;

    public HttpTransport(McpServerConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void connect() throws IOException {
        log.info("HTTP MCP transport connected to: {}", config.getUrl());
    }

    @Override
    public void disconnect() {
        log.info("HTTP MCP transport disconnected from: {}", config.getUrl());
    }

    @Override
    public String sendRequest(String jsonRpcRequest) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRpcRequest))
                .timeout(Duration.ofSeconds(30))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IOException("HTTP error " + response.statusCode() + ": " + response.body());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request interrupted", e);
        }
    }

    @Override
    public void sendNotification(String jsonRpcRequest) throws IOException {
        sendRequest(jsonRpcRequest);
    }

    @Override
    public boolean isConnected() {
        return config.getUrl() != null && !config.getUrl().isBlank();
    }
}
