package com.agent.cli;

import org.jline.reader.*;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class InputReader {

    private final LineReader lineReader;
    private volatile boolean eof = false;

    public InputReader() throws IOException {
        Path historyDir = Paths.get(System.getProperty("user.home"), ".my-agent");
        Files.createDirectories(historyDir);
        Path historyFile = historyDir.resolve("history");

        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        this.lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .history(new DefaultHistory())
                .completer(new StringsCompleter("/help", "/clear", "/exit", "/quit", "/cost", "/tools"))
                .build();

        this.lineReader.setVariable(LineReader.HISTORY_FILE, historyFile);
    }

    public String readLine() {
        StringBuilder buffer = new StringBuilder();
        boolean firstLine = true;

        while (true) {
            String prompt = firstLine ? "you> " : "> ";
            String line;
            try {
                line = lineReader.readLine(prompt);
            } catch (EndOfFileException e) {
                eof = true;
                return buffer.isEmpty() ? null : buffer.toString().trim();
            } catch (UserInterruptException e) {
                if (buffer.isEmpty()) {
                    System.out.println("\n(输入已取消)");
                    return "";
                }
                buffer.setLength(0);
                firstLine = true;
                System.out.println("\n(多行输入已取消)");
                continue;
            }

            if (line == null) {
                eof = true;
                return buffer.isEmpty() ? null : buffer.toString().trim();
            }

            // 续行符 \ 结尾表示继续输入
            if (line.endsWith("\\")) {
                buffer.append(line, 0, line.length() - 1).append("\n");
                firstLine = false;
                continue;
            }

            // 空行结束多行输入
            if (!firstLine && line.trim().isEmpty()) {
                return buffer.toString().trim();
            }

            if (!firstLine) {
                buffer.append("\n");
            }
            buffer.append(line);

            // 单行直接返回
            if (firstLine) {
                return buffer.toString().trim();
            }

            firstLine = false;
        }
    }

    public boolean isEof() {
        return eof;
    }

    public Terminal getTerminal() {
        return lineReader.getTerminal();
    }

    public String readLineRaw() {
        try {
            return lineReader.readLine();
        } catch (EndOfFileException e) {
            eof = true;
            return null;
        } catch (UserInterruptException e) {
            return "";
        }
    }
}
