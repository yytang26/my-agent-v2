package com.agent.tool.code;

import com.agent.tool.fs.PathValidator;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class ProcessRunner {

    private final PathValidator pathValidator;

    public ProcessRunner(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    public ProcessResult run(String command, long timeoutMs) {
        Path workingDir = pathValidator.getWorkspaceRoot();

        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true);

        Process process;
        try {
            process = pb.start();
        } catch (Exception e) {
            return new ProcessResult(-1, "Failed to start process: " + e.getMessage(), false);
        }

        StringBuilder outputBuilder = new StringBuilder();
        Thread readerThread = new Thread(() -> {
            try (InputStream is = process.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line).append("\n");
                }
            } catch (Exception e) {
                outputBuilder.append("\n[Error reading output: ").append(e.getMessage()).append("]");
            }
        });
        readerThread.start();

        boolean timedOut;
        try {
            timedOut = !process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new ProcessResult(-1, "Process interrupted: " + e.getMessage(), false);
        }

        if (timedOut) {
            process.destroyForcibly();
            try {
                readerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new ProcessResult(-1, outputBuilder.toString(), true);
        }

        try {
            readerThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int exitCode = process.exitValue();
        return new ProcessResult(exitCode, outputBuilder.toString(), false);
    }

    public record ProcessResult(int exitCode, String output, boolean timedOut) {
    }
}
