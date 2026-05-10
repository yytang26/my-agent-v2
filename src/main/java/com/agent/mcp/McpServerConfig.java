package com.agent.mcp;

import java.util.List;
import java.util.Map;

public class McpServerConfig {

    private String name;
    private String command;
    private List<String> args;
    private Map<String, String> env;
    private String transportType = "stdio";
    private String url;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "McpServerConfig{name='" + name + "', command='" + command + "', transportType='" + transportType + "'}";
    }
}
