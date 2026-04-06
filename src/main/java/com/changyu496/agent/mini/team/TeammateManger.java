package com.changyu496.agent.mini.team;

import com.changyu496.agent.mini.OpenAIHttpClient;
import com.changyu496.agent.mini.agent.AgentRunner;
import com.changyu496.agent.mini.agent.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private static TeammateManger instance;

    private static final Map<String, Map<String, Object>> shutdownRequests = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> planRequests = new ConcurrentHashMap<>();

    private TeammateManger(Path teamDir) {
        configPath = teamDir.resolve("config.json");
        inboxDirPath = teamDir.resolve("inbox");
        this.messageBus = new MessageBus(inboxDirPath);
        loadConfig(configPath);
    }

    public static Map<String, Map<String, Object>> getShutdownRequests() {
        return shutdownRequests;
    }

    public static Map<String, Map<String, Object>> getPlanRequests() {
        return planRequests;
    }

    public MessageBus getMessageBus() {
        return messageBus;
    }

    public static synchronized TeammateManger getInstance(Path teamDir) {
        if (instance == null) {
            instance = new TeammateManger(teamDir);
        }
        return instance;
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
        system.setContent("你是" + name + "你的角色设定为" + role + "。完成任务后，使用 send_message 工具把结果发给 lead。");
        messages.add(system);
        Message initTask = new Message();
        initTask.setRole("user");
        initTask.setContent(prompt);
        messages.add(initTask);

        int maxRound = 50;
        int round = 0;

        AtomicBoolean shouldExit = new AtomicBoolean(false);  // ← 新增

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

                if ("shutdown_request".equals(msg.get("type"))) {
                    String requestId = String.valueOf(msg.get("request_id"));
                    Map<String, Object> req = new HashMap<>();
                    req.put("status", "approved");
                    shutdownRequests.put(requestId, req);
                    Map<String, Object> extra = new HashMap<>();
                    extra.put("request_id", requestId);
                    extra.put("approve", true);
                    messageBus.send(name, "lead", "Shutting down gracefully", "shutdown_request", extra);
                    shouldExit.set(true);
                    break;
                }
            }
            // LLM
            String result = AgentRunner.run(
                    OpenAIHttpClient.getInstance(),
                    messages,
                    buildTeammateTools(),
                    getTeammateExecutor(name, shouldExit),
                    null,
                    maxRound);
            if (result != null && !result.isEmpty()) {
                List<Map<String, Object>> nextInbox = messageBus.readInbox(name);
                if (nextInbox.isEmpty()) {
                    Member m = findMember(name);
                    if (m != null) {
                        m.setStatus("idle");
                        saveConfig();
                    }
                    break;
                }
            }
            if (shouldExit.get()) {
                break;
            }
            Member member = findMember(name);
            if (member != null && "shutdown".equals(member.getStatus())) {
                break;
            }
            round++;
        }
    }

    public AgentRunner.ToolExecutor getTeammateExecutor(String name, AtomicBoolean shouldExit) {
        return (toolName, argsJson) -> {
            switch (toolName) {
                case "read_file": {
                    // argsJson 格式: {"path": "/some/file.txt"}
                    Map<String, String> args = parseArgs(argsJson);
                    String path = args.get("path");
                    try {
                        return Files.readString(Path.of(path));
                    } catch (Exception e) {
                        return "读取文件失败: " + e.getMessage();
                    }
                }
                case "write_file": {
                    Map<String, String> args = parseArgs(argsJson);
                    String path = args.get("path");
                    String content = args.get("content");
                    try {
                        Files.writeString(Path.of(path), content);
                        return "写入成功: " + path;
                    } catch (Exception e) {
                        return "写入文件失败: " + e.getMessage();
                    }
                }
                case "bash": {
                    Map<String, String> args = parseArgs(argsJson);
                    String command = args.get("command");
                    try {
                        Process p = Runtime.getRuntime().exec(command);
                        String output = new String(p.getInputStream().readAllBytes());
                        String error = new String(p.getErrorStream().readAllBytes());
                        return output + error;
                    } catch (Exception e) {
                        return "命令执行失败: " + e.getMessage();
                    }
                }
                case "send_message": {
                    // argsJson 格式: {"to": "alice", "content": "hello"}
                    Map<String, String> args = parseArgs(argsJson);
                    String to = args.get("to");
                    String content = args.get("content");
                    return messageBus.send(name, to, content, "message");
                }
                case "shutdown_response": {
                    Map<String, String> args = parseArgs(argsJson);
                    String requestId = args.get("request_id");
                    boolean approve = Boolean.parseBoolean(args.get("approve"));
                    Map<String, Object> req = new HashMap<>();
                    req.put("status", approve ? "approved" : "rejected");
                    shutdownRequests.put(requestId, req);
                    Map<String, Object> extra = new HashMap<>();
                    extra.put("request_id", requestId);
                    extra.put("approve", approve);
                    messageBus.send(name, "lead", args.getOrDefault("reason", ""), "shutdown_response", extra);
                    if (approve) {
                        shouldExit.set(true);
                    }
                    return "Shutdown " + (approve ? "approved" : "rejected");
                }
                case "plan_approve": {
                    Map<String, String> args = parseArgs(argsJson);
                    String plan = args.get("plan");
                    String requestId = UUID.randomUUID().toString().substring(0, 8);
                    Map<String, Object> req = new HashMap<>();
                    req.put("from", name);
                    req.put("plan", plan);
                    req.put("status", "pending");
                    planRequests.put(requestId, req);
                    // 发给lead
                    Map<String, Object> extra = new HashMap<>();
                    extra.put("request_id", requestId);
                    extra.put("plan", plan);
                    messageBus.send(name, "lead", plan, "plan_approval_response", extra);
                    return "Plan submitted (id=" + requestId + "), waiting for approval";
                }
                default:
                    return "未知工具: " + toolName;
            }
        };
    }

    /**
     * Teammate 可用的工具列表（受限子集）
     */
    public static List<Map<String, Object>> buildTeammateTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(tool("read_file", "读取文件",
                Map.of("path", Map.of("type", "string", "description", "文件路径"))));

        tools.add(tool("write_file", "写入文件",
                Map.of(
                        "path", Map.of("type", "string", "description", "文件路径"),
                        "content", Map.of("type", "string", "description", "写入内容")
                )));

        tools.add(tool("bash", "执行Shell命令",
                Map.of("command", Map.of("type", "string", "description", "Shell命令"))));

        tools.add(tool("send_message", "给队友发消息",
                Map.of(
                        "to", Map.of("type", "string", "description", "队友名字"),
                        "content", Map.of("type", "string", "description", "消息内容")
                )));

        tools.add(tool("shutdown_response", "响应关闭请求",
                Map.of(
                        "request_id", Map.of("type", "string", "description", "请求ID"),
                        "approve", Map.of("type", "boolean", "description", "true同意，false拒绝"),
                        "reason", Map.of("type", "string", "description", "理由")
                )));
        tools.add(tool("plan_approve", "提交计划给Leader审批",
                Map.of("plan", Map.of("type", "string", "description", "计划内容"))));

        return tools;
    }

    private Map<String, String> parseArgs(String argsJson) {
        Map<String, String> result = new HashMap<>();
        try {
            Map<String, Object> map = new ObjectMapper().readValue(argsJson, Map.class);
            for (Map.Entry<String, Object> e : map.entrySet()) {
                result.put(e.getKey(), String.valueOf(e.getValue()));
            }
        } catch (Exception e) {
            // 解析失败返回空 map
        }
        return result;
    }

    private static Map<String, Object> tool(String name, String description,
                                            Map<String, Object> properties) {
        Map<String, Object> function = new HashMap<>();      // ← 新增
        function.put("name", name);
        function.put("description", description);
        Map<String, Object> params = new HashMap<>();
        params.put("type", "object");
        params.put("properties", properties);
        function.put("parameters", params);                    // ← 移到 function 里

        Map<String, Object> tool = new HashMap<>();          // ← 新增外层包装
        tool.put("type", "function");
        tool.put("function", function);
        return tool;                                          // ← 返回包装后的
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
