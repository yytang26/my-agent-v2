package com.agent.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class SkillRegistry {

    private static final Logger log = LoggerFactory.getLogger(SkillRegistry.class);

    private final Map<String, PromptTemplate> skills = new LinkedHashMap<>();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @PostConstruct
    public void init() {
        loadSkills();
    }

    public void loadSkills() {
        skills.clear();

        // 加载内置技能 (classpath)
        loadClasspathSkills();

        // 加载用户自定义技能 (~/.my-agent/skills/)
        Path userSkillsDir = Paths.get(System.getProperty("user.home"), ".my-agent", "skills");
        if (Files.exists(userSkillsDir)) {
            loadDirectorySkills(userSkillsDir);
        }

        // 加载项目本地技能 (./.my-agent/skills/)
        Path localSkillsDir = Paths.get(".my-agent", "skills");
        if (Files.exists(localSkillsDir)) {
            loadDirectorySkills(localSkillsDir);
        }

        log.info("Skill registry loaded. Total skills: {}", skills.size());
    }

    private void loadClasspathSkills() {
        try {
            var resource = getClass().getClassLoader().getResource("skills");
            if (resource == null) {
                return;
            }
            Path path = Paths.get(resource.toURI());
            loadDirectorySkills(path);
        } catch (Exception e) {
            log.warn("Failed to load classpath skills: {}", e.getMessage());
        }
    }

    private void loadDirectorySkills(Path dir) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.yml")) {
            for (Path file : stream) {
                try {
                    PromptTemplate skill = yamlMapper.readValue(file.toFile(), PromptTemplate.class);
                    if (skill.getName() != null && !skill.getName().isBlank()) {
                        skills.put(skill.getName(), skill);
                        log.info("Loaded skill: {} from {}", skill.getName(), file);
                    }
                } catch (IOException e) {
                    log.warn("Failed to load skill file {}: {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to list skill files in {}: {}", dir, e.getMessage());
        }
    }

    public List<PromptTemplate> getSkills() {
        return new ArrayList<>(skills.values());
    }

    public Optional<PromptTemplate> getSkill(String name) {
        return Optional.ofNullable(skills.get(name));
    }

    public void reload() {
        loadSkills();
    }
}
