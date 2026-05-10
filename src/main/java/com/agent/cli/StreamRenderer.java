package com.agent.cli;

import com.agent.llm.model.StreamChunk;
import org.springframework.stereotype.Component;

@Component
public class StreamRenderer {

    public void renderChunk(StreamChunk chunk) {
        if (chunk.getText() != null) {
            System.out.print(chunk.getText());
            System.out.flush();
        }
    }

    public void renderToolStart(String toolName) {
        System.out.println();
        System.out.println("\uD83D\uDD27 调用工具: " + toolName);
        System.out.flush();
    }

    public void renderToolResult(String result) {
        System.out.println("\u2705 工具结果: " + result);
        System.out.flush();
    }

    public void newLine() {
        System.out.println();
        System.out.flush();
    }
}
