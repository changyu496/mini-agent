package com.changyu496.agent.mini.team;

import com.changyu496.agent.mini.dto.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TeammateManger {

    private TeamConfig teamConfig;
    private Path configPath;
    private Path inboxDirPath;
    private MessageBus messageBus;

    private final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * 运行中的线程（name -> Thread）
     */
    private Map<String, Thread> threads = new ConcurrentHashMap<>();

    public TeammateManger(Path teamDir) {
        configPath = teamDir.resolve("config.json");
        inboxDirPath = teamDir.resolve("inbox");
        this.messageBus = new MessageBus(inboxDirPath);
        loadConfig(configPath);
    }

    private void loadConfig(Path configPath) {
        if (Files.exists(configPath)) {
            try {
                this.teamConfig = objectMapper.readValue(configPath.toFile(), TeamConfig.class);
            } catch (Exception e) {
                System.out.println("读取团队配置遇到异常" + e.getMessage());
                this.teamConfig = new TeamConfig();
            }
        } else {
            teamConfig = new TeamConfig();
            teamConfig.setMembers(new ArrayList<>());
            saveConfig();
        }
    }

    private void saveConfig() {
        try {
            objectMapper.writeValue(configPath.toFile(), teamConfig);
        } catch (Exception e) {
            System.out.println("保存配置失败: " + e.getMessage());
        }
    }

    private Member findMember(String name) {
        return teamConfig.getMembers().stream().filter(m -> m.getName().equals(name)).findFirst().orElse(null);
    }

    public String spawn(String name, String role, String prompt) {
        Member existing = findMember(name);
        if (existing != null) {
            if (Objects.equals(existing.getStatus(), "working")) {
                return name + "正在处在运行中，不能启动";
            } else {
                existing.setRole(role);
                existing.setStatus("working");
            }
        } else {
            Member member = new Member();
            member.setRole(role);
            member.setStatus("working");
            member.setName(name);
            teamConfig.getMembers().add(member);
        }
        saveConfig();

        Thread thread = new Thread(() -> teammateLoop(name, role, prompt));
        threads.put(name, thread);
        thread.start();
        return "Spawned " + name + "\nrole:" + role + "\nprompt:" + prompt;

    }

    private void teammateLoop(String name, String role, String prompt) {
        List<Message> messages = new ArrayList<>();
        Message system = new Message();
        system.setRole("system");
        system.setContent("你是" + name + "你的角色设定为" + role);
        messages.add(system);
        Message initTask = new Message();
        initTask.setRole("user");
        initTask.setContent(prompt);
        messages.add(initTask);

        int maxRound = 50;
        int round = 0;

        while (round < maxRound) {
            List<Map<String, Object>> inbox = messageBus.readInbox(name);
            for (Map<String, Object> msg : inbox) {
                try {
                    Message m = new Message();
                    m.setRole("user");
                    m.setContent(objectMapper.writeValueAsString(msg));
                    messages.add(m);
                } catch (JsonProcessingException e) {
                    System.out.println("读取收件箱内容解析遇到异常" + e.getMessage());
                }
            }
            // LLM

            Member member = findMember(name);
            if (member != null && "shutdown".equals(member.getStatus())) {
                break;
            }
            round++;
        }
    }

    public String listAll() {
        if (teamConfig.getMembers().size() < 1) {
            return "暂无团队成员";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("团队：").append(teamConfig.getTeamName()).append("\n");
        for (Member m : teamConfig.getMembers()) {
            stringBuilder.append("  ").append(m.getName()).append("role:").append(m.getRole()).append("status:").append(m.getStatus());
        }
        return stringBuilder.toString();
    }

    public List<String> memberNames() {
        return teamConfig.getMembers().stream().map(Member::getName).collect(Collectors.toList());
    }
}
