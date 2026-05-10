package com.agent.mcp;

import java.io.IOException;

public interface McpTransport {
    void connect() throws IOException;
    void disconnect();
    String sendRequest(String jsonRpcRequest) throws IOException;
    void sendNotification(String jsonRpcRequest) throws IOException;
    boolean isConnected();
}
