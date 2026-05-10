package com.agent.react;

import org.springframework.stereotype.Component;

@Component
public class AgentMode {

    public static final String NATIVE = "native";
    public static final String REACT = "react";

    private volatile String currentMode = NATIVE;

    public String getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(String mode) {
        this.currentMode = mode;
    }

    public boolean isReact() {
        return REACT.equals(currentMode);
    }

    public boolean isNative() {
        return NATIVE.equals(currentMode);
    }
}
