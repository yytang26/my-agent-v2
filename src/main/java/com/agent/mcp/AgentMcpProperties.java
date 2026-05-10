package com.agent.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "agent.mcp")
public class AgentMcpProperties {

    private List<McpServerConfig> servers = new ArrayList<>();

    public List<McpServerConfig> getServers() {
        return servers;
    }

    public void setServers(List<McpServerConfig> servers) {
        this.servers = servers;
    }
}
