package com.agent.react;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

@Component
public class LoopDetector {

    private static final int WINDOW_SIZE = 5;
    private static final int LOOP_THRESHOLD = 3;

    private final Deque<String> actionHistory = new ArrayDeque<>();

    public synchronized void recordAction(String action) {
        if (action == null || action.isBlank()) {
            return;
        }
        actionHistory.addLast(action);
        if (actionHistory.size() > WINDOW_SIZE) {
            actionHistory.removeFirst();
        }
    }

    public synchronized boolean isLooping() {
        if (actionHistory.size() < LOOP_THRESHOLD) {
            return false;
        }
        String[] recent = actionHistory.toArray(new String[0]);
        int start = recent.length - LOOP_THRESHOLD;
        String first = recent[start];
        for (int i = start + 1; i < recent.length; i++) {
            if (!first.equals(recent[i])) {
                return false;
            }
        }
        return true;
    }

    public synchronized void reset() {
        actionHistory.clear();
    }
}
