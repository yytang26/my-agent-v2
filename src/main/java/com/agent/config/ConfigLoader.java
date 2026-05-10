package com.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

/**
 * 从指定路径加载 YAML 配置文件为 AgentConfig。
 * 文件不存在时返回空 AgentConfig（所有字段为 null）。
 */
public class ConfigLoader {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * 从指定路径加载 YAML 配置。
     *
     * @param path 配置文件路径
     * @return 解析后的 AgentConfig，文件不存在时返回空对象
     */
    public static AgentConfig load(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return new AgentConfig();
        }
        try {
            return YAML_MAPPER.readValue(file, AgentConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config from " + path, e);
        }
    }
}
