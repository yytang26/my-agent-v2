package com.agent.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class McpClient {

    private static final Logger log = LoggerFactory.getLogger(McpClient.class);

    private final ObjectMapper objectMapper;
    private McpTransport transport;
    private final AtomicInteger requestId = new AtomicInteger(1);

    public McpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void initialize(McpServerConfig config) throws IOException {
        if ("http".equalsIgnoreCase(config.getTransportType())) {
            this.transport = new HttpTransport(config);
        } else {
            this.transport = new StdioTransport(config);
        }
        this.transport.connect();

        ObjectNode initParams = objectMapper.createObjectNode();
        initParams.put("protocolVersion", "2024-11-05");
        ObjectNode capabilities = objectMapper.createObjectNode();
        initParams.set("capabilities", capabilities);
        ObjectNode clientInfo = objectMapper.createObjectNode();
        clientInfo.put("name", "my-agent");
        clientInfo.put("version", "1.0.0");
        initParams.set("clientInfo", clientInfo);

        String response = sendRequest("initialize", initParams);
        log.info("MCP server [{}] initialized: {}", config.getName(), response);

        ObjectNode notifyParams = objectMapper.createObjectNode();
        sendNotification("notifications/initialized", notifyParams);
    }

    public List<McpToolInfo> listTools() throws IOException {
        String response = sendRequest("tools/list", null);
        JsonNode root = objectMapper.readTree(response);
        JsonNode result = root.path("result");
        JsonNode toolsNode = result.path("tools");

        List<McpToolInfo> tools = new ArrayList<>();
        if (toolsNode.isArray()) {
            for (JsonNode toolNode : toolsNode) {
                String name = toolNode.path("name").asText();
                String description = toolNode.path("description").asText();
                JsonNode schemaNode = toolNode.path("inputSchema");
                Map<String, Object> inputSchema = objectMapper.convertValue(schemaNode, new TypeReference<>() {});
                tools.add(new McpToolInfo(name, description, inputSchema));
            }
        }
        return tools;
    }

    public String callTool(String name, Map<String, Object> arguments) throws IOException {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", name);
        if (arguments != null) {
            params.set("arguments", objectMapper.valueToTree(arguments));
        }
        String response = sendRequest("tools/call", params);
        JsonNode root = objectMapper.readTree(response);
        JsonNode result = root.path("result");
        if (result.has("content") && result.path("content").isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode content : result.path("content")) {
                if (content.has("text")) {
                    sb.append(content.path("text").asText());
                }
            }
            return sb.toString();
        }
        return result.toString();
    }

    public void close() {
        if (transport != null) {
            transport.disconnect();
            transport = null;
        }
    }

    private String sendRequest(String method, JsonNode params) throws IOException {
        int id = requestId.getAndIncrement();
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        request.put("id", id);
        if (params != null) {
            request.set("params", params);
        }
        String json = objectMapper.writeValueAsString(request);
        log.debug("MCP request: {}", json);
        String response = transport.sendRequest(json);
        log.debug("MCP response: {}", response);
        return response;
    }

    private void sendNotification(String method, JsonNode params) throws IOException {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        if (params != null) {
            request.set("params", params);
        }
        String json = objectMapper.writeValueAsString(request);
        transport.sendNotification(json);
    }

    public record McpToolInfo(String name, String description, Map<String, Object> inputSchema) {
    }
}
