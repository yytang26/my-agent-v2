package com.agent.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;

/**
 * 配置管理器，负责按优先级加载并合并多层配置。
 * <p>
 * 优先级从低到高：
 * 1. GLOBAL:   ~/.my-agent/config.yml
 * 2. PROJECT:  {workDir}/.my-agent/config.yml
 * 3. CLI:      Spring Environment / @Value
 * 4. SESSION:  运行时覆盖
 */
@Component
public class ConfigManager {

    @Value("${agent.config.work-dir:.}")
    private String workDir;

    private final Environment environment;
    private final Map<ConfigSource, AgentConfig> sourceConfigs = new EnumMap<>(ConfigSource.class);
    private final AgentConfig sessionOverride = new AgentConfig();

    public ConfigManager(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * 重新从文件加载 GLOBAL 和 PROJECT 配置，并合并 CLI 配置。
     */
    public void reload() {
        sourceConfigs.clear();

        // 1. GLOBAL
        String globalPath = Paths.get(System.getProperty("user.home"), ".my-agent", "config.yml").toString();
        AgentConfig globalConfig = ConfigLoader.load(globalPath);
        sourceConfigs.put(ConfigSource.GLOBAL, globalConfig);

        // 2. PROJECT
        Path projectPath = Paths.get(workDir, ".my-agent", "config.yml").toAbsolutePath().normalize();
        AgentConfig projectConfig = ConfigLoader.load(projectPath.toString());
        sourceConfigs.put(ConfigSource.PROJECT, projectConfig);

        // 3. CLI (从 Spring Environment 读取)
        AgentConfig cliConfig = loadCliConfig();
        sourceConfigs.put(ConfigSource.CLI, cliConfig);

        // 4. SESSION (运行时覆盖，保留当前值)
        sourceConfigs.put(ConfigSource.SESSION, sessionOverride);
    }

    private AgentConfig loadCliConfig() {
        AgentConfig cli = new AgentConfig();
        cli.setLlmProvider(getEnvProperty("llm.provider"));
        cli.setModel(getEnvProperty("claude.model"));
        String temp = getEnvProperty("claude.temperature");
        if (temp != null) {
            try {
                cli.setTemperature(Double.parseDouble(temp));
            } catch (NumberFormatException ignored) {
            }
        }
        String maxTokens = getEnvProperty("claude.max-tokens");
        if (maxTokens != null) {
            try {
                cli.setMaxTokens(Integer.parseInt(maxTokens));
            } catch (NumberFormatException ignored) {
            }
        }
        cli.setApiKey(getEnvProperty("claude.api-key"));
        cli.setBaseUrl(getEnvProperty("claude.base-url"));
        return cli;
    }

    private String getEnvProperty(String key) {
        String value = environment.getProperty(key);
        if (value != null && value.isEmpty()) {
            return null;
        }
        return value;
    }

    /**
     * 获取最终合并后的配置。
     *
     * @return 按优先级合并后的 AgentConfig
     */
    public AgentConfig getConfig() {
        AgentConfig merged = new AgentConfig();
        for (ConfigSource source : ConfigSource.values()) {
            AgentConfig config = sourceConfigs.get(source);
            if (config != null) {
                merged.merge(config);
            }
        }
        return merged;
    }

    /**
     * 运行时设置 Session 级别的配置覆盖。
     *
     * @param key   配置项名称
     * @param value 配置值
     */
    public void setSessionOverride(String key, String value) {
        switch (key) {
            case "llmProvider" -> sessionOverride.setLlmProvider(value);
            case "model" -> sessionOverride.setModel(value);
            case "apiKey" -> sessionOverride.setApiKey(value);
            case "baseUrl" -> sessionOverride.setBaseUrl(value);
            case "temperature" -> {
                try {
                    sessionOverride.setTemperature(Double.parseDouble(value));
                } catch (NumberFormatException ignored) {
                }
            }
            case "maxTokens" -> {
                try {
                    sessionOverride.setMaxTokens(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    /**
     * 获取指定来源的配置。
     *
     * @param source 配置来源
     * @return 该来源的配置
     */
    public AgentConfig getSourceConfig(ConfigSource source) {
        return sourceConfigs.getOrDefault(source, new AgentConfig());
    }

    /**
     * 打印当前各层配置来源的生效情况。
     */
    public void printConfigSources() {
        System.out.println("=== 配置来源层级 ===");
        for (ConfigSource source : ConfigSource.values()) {
            AgentConfig config = sourceConfigs.get(source);
            boolean hasValue = config != null && hasAnyValue(config);
            System.out.printf("  [%s] %s%n", source, hasValue ? "已生效" : "未设置");
        }
        System.out.println("==================");
    }

    private boolean hasAnyValue(AgentConfig config) {
        return config.getLlmProvider() != null
                || config.getModel() != null
                || config.getTemperature() != null
                || config.getMaxTokens() != null
                || config.getApiKey() != null
                || config.getBaseUrl() != null;
    }
}
