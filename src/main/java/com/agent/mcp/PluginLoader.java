package com.agent.mcp;

import com.agent.tool.ToolDefinition;
import com.agent.tool.ToolRegistry;
import com.agent.tool.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
public class PluginLoader {

    private static final Logger log = LoggerFactory.getLogger(PluginLoader.class);

    private final ToolRegistry toolRegistry;
    private final McpToolAdapter adapter;
    private final ObjectMapper objectMapper;
    private final List<McpServerConfig> configs;
    private final Map<String, McpClient> clients = new ConcurrentHashMap<>();
    private final Map<String, List<String>> serverTools = new ConcurrentHashMap<>();

    public PluginLoader(ToolRegistry toolRegistry,
                        McpToolAdapter adapter,
                        ObjectMapper objectMapper,
                        AgentMcpProperties mcpProperties) {
        this.toolRegistry = toolRegistry;
        this.adapter = adapter;
        this.objectMapper = objectMapper;
        this.configs = mcpProperties.getServers() != null ? mcpProperties.getServers() : List.of();
    }

    @PostConstruct
    public void init() {
        if (configs.isEmpty()) {
            log.info("No MCP servers configured, skipping plugin loading");
            return;
        }
        loadPlugins(configs);
    }

    public void loadPlugins(List<McpServerConfig> configs) {
        for (McpServerConfig config : configs) {
            try {
                loadServer(config);
            } catch (Exception e) {
                log.error("Failed to load MCP server [{}]: {}", config.getName(), e.getMessage(), e);
            }
        }
    }

    public void loadServer(McpServerConfig config) throws IOException {
        log.info("Loading MCP server: {} (transport: {})", config.getName(), config.getTransportType());
        McpClient client = new McpClient(objectMapper);
        client.initialize(config);

        List<McpClient.McpToolInfo> tools = client.listTools();
        List<String> registered = new ArrayList<>();

        for (McpClient.McpToolInfo toolInfo : tools) {
            ToolDefinition def = adapter.toToolDefinition(toolInfo);
            String toolName = def.getName();
            toolRegistry.registerDynamic(toolName, def,
                    args -> adapter.execute(client, toolInfo.name(), "mcp-" + toolName, args));
            registered.add(toolName);
            log.info("Registered dynamic tool from MCP [{}]: {}", config.getName(), toolName);
        }

        clients.put(config.getName(), client);
        serverTools.put(config.getName(), registered);
        log.info("MCP server [{}] loaded successfully with {} tools", config.getName(), registered.size());
    }

    public void unloadAll() {
        for (Map.Entry<String, McpClient> entry : clients.entrySet()) {
            try {
                entry.getValue().close();
                log.info("MCP server [{}] disconnected", entry.getKey());
            } catch (Exception e) {
                log.error("Error disconnecting MCP server [{}]: {}", entry.getKey(), e.getMessage());
            }
        }
        clients.clear();
        serverTools.clear();
    }

    public void reload() {
        log.info("Reloading all MCP plugins...");
        unloadAll();
        loadPlugins(configs);
    }

    public Map<String, List<String>> getLoadedPlugins() {
        return Map.copyOf(serverTools);
    }

    public int getTotalTools() {
        return serverTools.values().stream().mapToInt(List::size).sum();
    }
}
