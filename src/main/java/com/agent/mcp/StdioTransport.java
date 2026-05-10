package com.agent.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class StdioTransport implements McpTransport {

    private static final Logger log = LoggerFactory.getLogger(StdioTransport.class);
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

    private final McpServerConfig config;
    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    private ExecutorService executor;

    public StdioTransport(McpServerConfig config) {
        this.config = config;
    }

    @Override
    public void connect() throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(config.getCommand());
        if (config.getArgs() != null) {
            cmd.addAll(config.getArgs());
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (config.getEnv() != null) {
            pb.environment().putAll(config.getEnv());
        }

        log.info("Starting MCP server process: {}", String.join(" ", cmd));
        this.process = pb.start();
        this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "mcp-stdio-" + config.getName());
            t.setDaemon(true);
            return t;
        });

        Thread stderrDrainer = new Thread(() -> {
            try (BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = stderr.readLine()) != null) {
                    log.debug("MCP server [{}] stderr: {}", config.getName(), line);
                }
            } catch (IOException e) {
                // ignore on close
            }
        }, "mcp-stderr-" + config.getName());
        stderrDrainer.setDaemon(true);
        stderrDrainer.start();

        log.info("MCP server [{}] process started", config.getName());
    }

    @Override
    public void disconnect() {
        if (executor != null) {
            executor.shutdownNow();
        }
        if (process != null) {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        closeQuietly(writer);
        closeQuietly(reader);
        log.info("MCP server [{}] disconnected", config.getName());
    }

    @Override
    public String sendRequest(String jsonRpcRequest) throws IOException {
        if (!isConnected()) {
            throw new IOException("MCP transport not connected for server: " + config.getName());
        }

        synchronized (this) {
            writer.write(jsonRpcRequest);
            writer.newLine();
            writer.flush();

            Future<String> future = executor.submit(reader::readLine);
            try {
                String response = future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (response == null) {
                    throw new IOException("MCP server [" + config.getName() + "] closed stdout unexpectedly");
                }
                return response;
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new IOException("MCP request timed out after " + REQUEST_TIMEOUT_SECONDS + "s", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("MCP request interrupted", e);
            } catch (ExecutionException e) {
                throw new IOException("MCP request failed: " + e.getCause().getMessage(), e.getCause());
            }
        }
    }

    @Override
    public void sendNotification(String jsonRpcRequest) throws IOException {
        if (!isConnected()) {
            throw new IOException("MCP transport not connected for server: " + config.getName());
        }
        synchronized (this) {
            writer.write(jsonRpcRequest);
            writer.newLine();
            writer.flush();
        }
    }

    @Override
    public boolean isConnected() {
        return process != null && process.isAlive();
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
