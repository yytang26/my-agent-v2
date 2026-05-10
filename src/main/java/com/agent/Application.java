package com.agent;

import com.agent.cli.CliRepl;
import com.agent.mcp.AgentMcpProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(AgentMcpProperties.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @ConditionalOnExpression("'${agent.mode:web}'.equals('cli') || '${agent.mode:web}'.equals('both')")
    public CommandLineRunner run(CliRepl cliRepl, @Value("${agent.mode:web}") String mode) {
        return args -> {
            if ("both".equals(mode)) {
                Thread cliThread = new Thread(cliRepl::start);
                cliThread.setDaemon(true);
                cliThread.start();
            } else {
                cliRepl.start();
            }
        };
    }
}
