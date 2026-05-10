package com.agent.routing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "agent.routing")
public class RoutingConfig {

    private boolean enabled = false;
    private boolean useLlm = false;

    private Map<Intent, List<String>> intentTools = Map.of(
            Intent.GENERAL, List.of(),
            Intent.CODE, List.of("grep", "glob", "read_file", "edit_file", "bash"),
            Intent.FILE, List.of("read_file", "write_file", "edit_file", "list_directory"),
            Intent.SEARCH, List.of("grep", "glob", "list_directory"),
            Intent.SYSTEM, List.of("bash")
    );

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isUseLlm() {
        return useLlm;
    }

    public void setUseLlm(boolean useLlm) {
        this.useLlm = useLlm;
    }

    public Map<Intent, List<String>> getIntentTools() {
        return intentTools;
    }

    public void setIntentTools(Map<Intent, List<String>> intentTools) {
        this.intentTools = intentTools;
    }

    public List<String> getToolsForIntent(Intent intent) {
        return intentTools.getOrDefault(intent, List.of());
    }
}
