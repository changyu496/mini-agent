package com.changyu496.agent.mini.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SkillLoader {

    public static final String SKILLS_DIR = "src/main/resources/skills";

    private static final SkillLoader instance = new SkillLoader();

    private final Map<String, String> skills = new HashMap<>();

    public static SkillLoader getInstance() {
        return instance;
    }

    private SkillLoader() {
        loadSkills();
    }

    private void loadSkills() {
        Path dir = Path.of(SKILLS_DIR);
        if (!Files.exists(dir)) {
            return;
        }
        try {
            Files.list(dir).filter(Files::isDirectory).forEach(skillDir -> {
                Path skillFile = skillDir.resolve("SKILL.md");
                if (Files.exists(skillFile)) {
                    String content = null;
                    try {
                        content = Files.readString(skillFile);
                        String body = parseBody(content);
                        skills.put(skillDir.getFileName().toString(), body);
                    } catch (IOException e) {
                        System.out.println("加载技能失败:" + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            System.out.println("加载技能失败:" + e.getMessage());
        }
    }

    private String parseBody(String content) {
        if (content.startsWith("---")) {
            int end = content.indexOf("---", 3);
            if (end != -1) {
                return content.substring(end + 3).trim();
            }
        }
        return content.trim();
    }

    public String getDescriptions() {
        if (skills.isEmpty()) {
            return "(无可用技能)";
        }
        StringBuilder sb = new StringBuilder();
        skills.forEach((name, body) -> sb.append("- ").append(name).append("\n"));
        return sb.toString();
    }

    public String getContent(String name) {
        String body = skills.get(name);
        if (body == null) {
            return "错误：未找到技能:" + name;
        }
        return "<skill name=\"" + name + "\">\n" + body + "\n</skill>";
    }
}
