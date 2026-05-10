package com.agent.cli;

import org.springframework.stereotype.Component;

@Component
public class Spinner {

    private static final String[] FRAMES = {
        "\u280B", "\u2819", "\u2839", "\u2838", "\u283C", "\u2834", "\u2826", "\u2827", "\u2807", "\u280F"
    };
    private static final String TEXT = " 思考中...";

    private Thread spinnerThread;
    private volatile boolean running = false;

    public void start() {
        if (running) {
            return;
        }
        running = true;
        spinnerThread = new Thread(() -> {
            int frameIndex = 0;
            while (running) {
                String frame = FRAMES[frameIndex % FRAMES.length];
                System.out.print("\r\033[K" + frame + TEXT);
                System.out.flush();
                frameIndex++;
                try {
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        spinnerThread.setDaemon(true);
        spinnerThread.start();
    }

    public void stop() {
        stop(null);
    }

    public void stop(String message) {
        running = false;
        if (spinnerThread != null) {
            spinnerThread.interrupt();
            try {
                spinnerThread.join(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.print("\r\033[K");
        System.out.flush();
        if (message != null) {
            System.out.println(message);
        }
    }
}
