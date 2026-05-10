package com.agent.config;

/**
 * 配置来源枚举，定义了配置加载的优先级层级。
 * 优先级从低到高，高优先级的配置会覆盖低优先级的配置。
 */
public enum ConfigSource {
    GLOBAL,   // ~/.my-agent/config.yml
    USER,     // 用户级（预留）
    PROJECT,  // ./.my-agent/config.yml
    CLI,      // 启动参数 / Spring Environment
    SESSION   // 运行时覆盖
}
