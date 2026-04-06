package com.changyu496.agent.mini;

import com.changyu496.agent.mini.agent.AgentRunner;
import com.changyu496.agent.mini.agent.Message;
import com.changyu496.agent.mini.agent.OpenAIResponse;
import com.changyu496.agent.mini.background.BackgroundManger;
import com.changyu496.agent.mini.background.JobNotification;
import com.changyu496.agent.mini.todo.TodoManager;
import com.changyu496.agent.mini.tool.SkillLoader;
import com.changyu496.agent.mini.tool.TodoHandler;
import com.changyu496.agent.mini.tool.ToolDefinition;
import com.changyu496.agent.mini.tool.handler.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.nio.file.StandardOpenOption.APPEND;

public class Main {

    private static final Map<String, ToolDefinition> mainDispatcher = new HashMap<>();

    static {
        mainDispatcher.put("read_file", getReadFileDefinition());
        mainDispatcher.put("write_file", getWriteFileDefinition());
        mainDispatcher.put("search_notes", getSearchNotesDefinition());
        mainDispatcher.put("todo", getTodoDefinition());
        mainDispatcher.put("sub_agent", getSubAgentDefinition());
        mainDispatcher.put("load_skill", getLoadSkillDefinition());
        mainDispatcher.put("compact", getCompactDefinition());
        mainDispatcher.put("task_create", getTaskCreateDefinition());
        mainDispatcher.put("task_update", getTaskUpdateDefinition());
        mainDispatcher.put("task_list", getTaskListDefinition());
        mainDispatcher.put("task_detail", getTaskDetailDefinition());
        mainDispatcher.put("background_submit", getBackgroundSubmitDefinition());
        mainDispatcher.put("background_check", getBackgroundCheckDefinition());
        mainDispatcher.put("spawn_teammate", getSpawnTeammateDefinition());
        mainDispatcher.put("list_teammates", getListTeammatesDefinition());
        mainDispatcher.put("read_inbox", getReadInboxDefinition());
        mainDispatcher.put("send_message", getSendMessageDefinition());
        mainDispatcher.put("broadcast", getBroadcastDefinition());
    }

    private static final Map<String, ToolDefinition> subAgentDispatcher = new HashMap<>();

    static {
        subAgentDispatcher.put("read_file", getReadFileDefinition());
        subAgentDispatcher.put("write_file", getWriteFileDefinition());
        subAgentDispatcher.put("search_notes", getSearchNotesDefinition());
        subAgentDispatcher.put("todo", getTodoDefinition());
        subAgentDispatcher.put("load_skill", getLoadSkillDefinition());
        subAgentDispatcher.put("compact", getCompactDefinition());
        subAgentDispatcher.put("task_create", getTaskCreateDefinition());
        subAgentDispatcher.put("task_update", getTaskUpdateDefinition());
        subAgentDispatcher.put("task_list", getTaskListDefinition());
        subAgentDispatcher.put("task_detail", getTaskDetailDefinition());
    }

    private static final int MAX_MESSAGE_SIZE = 100;

    public static List<Message> historyMessages = new ArrayList<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        buildSystemPrompt(historyMessages);
        int roundSinceTodo = 0;
        while (true) {
            System.out.print("mini agent >> ");
            String userInput = scanner.nextLine();
            if ("exit".equals(userInput) || "q".equals(userInput)) {
                break;
            }
            if (historyMessages.size() + 1 > MAX_MESSAGE_SIZE) {
                // 永远保留第一个system prompt
                historyMessages.remove(1);
            }
            Message message = new Message();
            message.setContent(userInput);
            message.setRole("user");
            historyMessages.add(message);
            try {
                AgentRunner.ToolExecutor mainExecutor = ((toolName, argsJson) -> {
                    ToolDefinition def = mainDispatcher.get(toolName);
                    return def.getToolHandler().execute(argsJson, toolName);
                });
                AgentRunner.NotificationHandler mainNotificationHandler = Main::injectBackgroundNotifications;
                String content = AgentRunner.run(OpenAIHttpClient.getInstance(), historyMessages, regTool(), mainExecutor, mainNotificationHandler, -1);
                System.out.println("assistant:" + content);
                microCompact(historyMessages);
                roundSinceTodo++;
                if (roundSinceTodo >= 3) {
                    Message reminder = new Message();
                    reminder.setRole("user");
                    reminder.setContent("<reminder>你已经3轮没有更新todo了，请调用todo工具</reminder>");
                    reminder.setToolCallId(null);
                    historyMessages.add(reminder);
                    roundSinceTodo = 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void injectBackgroundNotifications(List<Message> historyMessages) {
        List<JobNotification> jobNotifications = BackgroundManger.getInstance().drainNotifications();
        StringBuilder stringBuilder = new StringBuilder();
        if (Objects.nonNull(jobNotifications) && jobNotifications.size() > 0) {
            stringBuilder.append("目前Agent的后台任务状况情况").append("\n");
            jobNotifications.forEach(jobNotification -> {
                stringBuilder.append(jobNotification.render())
                        .append("\n");
            });
            // 如果有结果，就把内容放到消息列表里，让LLM去处理返回
            Message message = new Message();
            message.setRole("user");
            message.setContent(stringBuilder.toString());
            historyMessages.add(message);
        }
    }

    private static void microCompact(List<Message> historyMessages) {
        int toolResultCount = 0;
        for (int i = historyMessages.size() - 1; i >= 0; i--) {
            Message msg = historyMessages.get(i);
            if (msg.getRole().equals("tool")) {
                toolResultCount++;
                if (toolResultCount > 3) {
                    msg.setContent("[Previously used {" + historyMessages.get(i).getName() + "}]");
                }
            }
        }
    }

    public static List<Message> autoCompact(List<Message> historyMessages) {
        String DEFAULT_PATH = "/Users/changyu/.reading-agent/transcripts";
        // 获取当前时间，作为文件名
        String fileName = DEFAULT_PATH + "/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".md";
        Path filePath = Path.of(fileName);
        if (!Files.exists(filePath)) {
            try {
                Files.createFile(filePath);
            } catch (IOException e) {
                System.out.println("创建记忆文件失败，遇到异常" + e.getMessage());
                return historyMessages;
            }
        }
        // 把内容转成json写入
        StringBuilder stringBuilder = new StringBuilder();
        historyMessages.forEach(message -> stringBuilder.append(message.getRole())
                .append(":")
                .append(message.getContent())
                .append("\n"));
        try {
            Files.writeString(filePath, stringBuilder.toString(), APPEND);
        } catch (IOException e) {
            System.out.println("写入记忆文件失败，遇到异常" + e.getMessage());
            return historyMessages;
        }
        // 做摘要
        Message system = new Message();
        system.setRole("system");
        system.setContent("请根据内容帮我最摘要，返回[摘要信息]+[确认消息]");
        List<Message> summaryMessage = new ArrayList<>();
        Message user = new Message();
        user.setRole("user");
        user.setContent(stringBuilder.toString());
        summaryMessage.add(system);
        summaryMessage.add(user);
        OpenAIResponse openAIResponse = OpenAIHttpClient.getInstance().call(summaryMessage, new ArrayList<>());
        String summaryText = openAIResponse.getChoices().get(0).getMessage().getContent();
        List<Message> compact = new ArrayList<>();
        Message summary = new Message();
        summary.setRole("user");
        summary.setContent("【摘要】" + summaryText);
        Message confirm = new Message();
        confirm.setRole("assistant");
        confirm.setContent("好的，我记住了");
        compact.add(summary);
        compact.add(confirm);
        return compact;
    }

    private static void buildSubAgentSystemPrompt(List<Message> historyMessages) {
        Message system = new Message();
        system.setRole("system");
        String todoStatus = TodoManager.getInstance().render();
        system.setContent("你是一个专业的读书研究助手，专注于深入研究用户提出的问题。\n" + "你会收到一个具体的研究任务，请用搜索和阅读工具找到答案，\n" + "最后用清晰的语言总结你的发现。\n" + "【当前状态】\n" + todoStatus + "\n" + "只返回最终研究结论，不要重复工具调用的过程。\n" + "笔记目录：~/.reading-agent/workspace/");
        historyMessages.add(system);
    }

    public static String runSubAgent(String prompt) {
        List<Message> subMessages = new ArrayList<>();
        buildSubAgentSystemPrompt(subMessages);
        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent(prompt);
        subMessages.add(userMessage);
        AgentRunner.ToolExecutor subAgentExecutor = ((toolName, argsJson) -> {
            ToolDefinition def = mainDispatcher.get(toolName);
            return def.getToolHandler().execute(argsJson, toolName);
        });
        return AgentRunner.run(OpenAIHttpClient.getInstance(), subMessages, regBasicTool(), subAgentExecutor, null, 30);
    }

    private static void buildSystemPrompt(List<Message> historyMessages) {
        Message system = new Message();
        system.setRole("system");
        String todoStatus = TodoManager.getInstance().render();
        system.setContent("你是一个读书伴侣，专注于帮助用户深入理解读过的书。\n" + "你的核心能力：\n" + "- 读取用户的读书笔记（使用 read_file 工具）\n" + "- 写入和更新笔记（使用 write_file 工具）\n" + "- 管理待办和进度（使用 todo 工具）\n" + "- 基于笔记内容展开讨论和追问\n" + todoStatus + "\n" + "【可用技能】\n" + SkillLoader.getInstance().getDescriptions() + "\n" + "【关键规则】\n" + "- 用户提到更新读书进度（读到哪章、换书等）→ 必须调用 todo 工具，action=progress\n" + "- 用户提到添加待办、完成任务 → 必须调用 todo 工具，action=update\n" + "- 其他情况（读笔记、写笔记）才用 read_file / write_file\n" + "笔记目录：~/.reading-agent/workspace/\n" + "todo 文件：~/.reading-agent/workspace/todo.json");
        historyMessages.add(system);
    }

    private static List<Map<String, Object>> regTool() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(toolToMap(getReadFileDefinition()));
        tools.add(toolToMap(getWriteFileDefinition()));
        tools.add(toolToMap(getSearchNotesDefinition()));
        tools.add(toolToMap(getTodoDefinition()));
        tools.add(toolToMap(getSubAgentDefinition()));
        tools.add(toolToMap(getLoadSkillDefinition()));
        tools.add(toolToMap(getTaskCreateDefinition()));
        tools.add(toolToMap(getTaskUpdateDefinition()));
        tools.add(toolToMap(getTaskListDefinition()));
        tools.add(toolToMap(getTaskDetailDefinition()));
        tools.add(toolToMap(getBackgroundCheckDefinition()));
        tools.add(toolToMap(getBackgroundSubmitDefinition()));
        tools.add(toolToMap(getSpawnTeammateDefinition()));
        tools.add(toolToMap(getListTeammatesDefinition()));
        tools.add(toolToMap(getSendMessageDefinition()));
        tools.add(toolToMap(getReadInboxDefinition()));
        tools.add(toolToMap(getBroadcastDefinition()));

        return tools;
    }

    private static List<Map<String, Object>> regBasicTool() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(toolToMap(getReadFileDefinition()));
        tools.add(toolToMap(getWriteFileDefinition()));
        tools.add(toolToMap(getSearchNotesDefinition()));
        tools.add(toolToMap(getTodoDefinition()));
        tools.add(toolToMap(getLoadSkillDefinition()));
        return tools;
    }

    public static Map<String, Object> toolToMap(ToolDefinition toolDefinition) {
        Map<String, Object> toolMap = new HashMap<>();
        Map<String, Object> function = new HashMap<>();
        toolMap.put("function", function);
        toolMap.put("type", "function");
        function.put("name", toolDefinition.getName());
        function.put("description", toolDefinition.getDescription());
        function.put("parameters", toolDefinition.getParameterSchema());
        return toolMap;
    }


    public static ToolDefinition getReadFileDefinition() {
        Map<String, Object> readParams = new HashMap<>();
        Map<String, Object> readProps = new HashMap<>();
        Map<String, Object> path = new HashMap<>();
        path.put("type", "string");
        path.put("description", "要读取的文件路径");
        readProps.put("path", path);
        readParams.put("type", "object");
        readParams.put("properties", readProps);
        List<String> requiredList = new ArrayList<>();
        requiredList.add("path");
        readParams.put("required", requiredList);
        return new ToolDefinition("read_file", "读取文件内容", readParams, new ReadFileHandler());
    }

    public static ToolDefinition getWriteFileDefinition() {
        Map<String, Object> writeParams = new HashMap<>();
        Map<String, Object> writeProps = new HashMap<>();
        Map<String, Object> path = new HashMap<>();
        path.put("type", "string");
        path.put("description", "要写入的文件路径");
        Map<String, Object> content = new HashMap<>();
        content.put("type", "string");
        content.put("description", "要写入的内容");
        writeProps.put("path", path);
        writeProps.put("content", content);
        writeParams.put("type", "object");
        writeParams.put("properties", writeProps);
        List<String> requiredList = new ArrayList<>();
        requiredList.add("path");
        requiredList.add("content");
        writeParams.put("required", requiredList);
        return new ToolDefinition("write_file", "写入文件内容", writeParams, new WriteFileHandler());
    }

    public static ToolDefinition getSearchNotesDefinition() {
        Map<String, Object> searchNotesParams = new HashMap<>();
        Map<String, Object> searchNotesProps = new HashMap<>();
        Map<String, Object> keyword = new HashMap<>();
        keyword.put("type", "string");
        keyword.put("description", "搜索关键字");
        Map<String, Object> dir = new HashMap<>();
        dir.put("type", "string");
        dir.put("description", "搜索的目录路径地址");
        searchNotesProps.put("keyword", keyword);
        searchNotesProps.put("dir", dir);
        searchNotesParams.put("type", "object");
        searchNotesParams.put("properties", searchNotesProps);
        List<String> requiredList = new ArrayList<>();
        requiredList.add("keyword");
        searchNotesParams.put("required", requiredList);
        return new ToolDefinition("search_notes", "查找读书笔记", searchNotesParams, new SearchNotesHandler());
    }

    public static ToolDefinition getTodoDefinition() {
        Map<String, Object> todoParams = new HashMap<>();
        todoParams.put("type", "object");

        Map<String, Object> todoProps = new HashMap<>();

        Map<String, Object> action = new HashMap<>();
        action.put("type", "string");
        action.put("description", "操作类型 action 或 progress");
        todoProps.put("action", action);

        Map<String, Object> items = new HashMap<>();
        items.put("type", "array");
        items.put("description", "待办事项列表，每项格式：{id:string, text:string, status:string}，status可为pending/in_progress/completed，仅action=update时用");
        todoProps.put("items", items);

        Map<String, Object> book = new HashMap<>();
        book.put("type", "string");
        book.put("description", "当前在读的书名，仅 action = progress时用");
        todoProps.put("book", book);

        Map<String, Object> chapter = new HashMap<>();
        chapter.put("type", "integer");
        chapter.put("description", "当前章节号，仅 action = progress时用");
        todoProps.put("chapter", chapter);

        Map<String, Object> chapterTitle = new HashMap<>();
        chapterTitle.put("type", "string");
        chapterTitle.put("description", "章节标题，仅 action=progress 时用");
        todoProps.put("chapterTitle", chapterTitle);

        Map<String, Object> progress = new HashMap<>();
        progress.put("type", "string");
        progress.put("description", "当前进度，仅 action = progress时用");
        todoProps.put("progress", progress);

        todoParams.put("properties", todoProps);
        todoParams.put("required", List.of("action"));

        return new ToolDefinition("todo", "管理读书进度和待办事项", todoParams, new TodoHandler());
    }

    public static ToolDefinition getSubAgentDefinition() {
        Map<String, Object> subAgentParams = new HashMap<>();
        subAgentParams.put("type", "object");
        Map<String, Object> sugAgentProps = new HashMap<>();

        Map<String, Object> prompt = new HashMap<>();
        prompt.put("type", "string");
        prompt.put("description", "给子Agent的研究任务描述，要清晰具体");

        sugAgentProps.put("prompt", prompt);

        subAgentParams.put("properties", sugAgentProps);
        subAgentParams.put("required", List.of("prompt"));

        return new ToolDefinition("sub_agent", "启动一个子Agent，用新的上下文完成研究任务", subAgentParams, SubAgentHandler.getInstance());
    }

    public static ToolDefinition getLoadSkillDefinition() {
        Map<String, Object> loadSkillParams = new HashMap<>();
        loadSkillParams.put("type", "object");
        Map<String, Object> loadSkillProps = new HashMap<>();

        Map<String, Object> skillName = new HashMap<>();
        skillName.put("type", "string");
        skillName.put("description", "技能名称");

        loadSkillProps.put("name", skillName);

        loadSkillParams.put("properties", loadSkillProps);
        loadSkillParams.put("required", List.of("name"));

        return new ToolDefinition("load_skill", "获取技能", loadSkillParams, new LoadSkillHandler());
    }

    public static ToolDefinition getCompactDefinition() {
        Map<String, Object> compactParams = new HashMap<>();
        compactParams.put("type", "object");
        compactParams.put("properties", new HashMap<>());
        compactParams.put("required", new ArrayList<>());
        return new ToolDefinition("compact", "压缩对话", compactParams, new CompactHandler());
    }

    public static ToolDefinition getTaskCreateDefinition() {
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("type", "object");

        Map<String, Object> taskProps = new HashMap<>();

        Map<String, Object> subject = new HashMap<>();
        subject.put("type", "string");
        subject.put("description", "任务名称");
        taskProps.put("subject", subject);

        Map<String, Object> description = new HashMap<>();
        description.put("type", "string");
        description.put("description", "任务描述");
        taskProps.put("description", description);

        taskParams.put("properties", taskProps);
        taskParams.put("required", List.of("subject"));

        return new ToolDefinition("task_create", "任务创建", taskParams, new TaskHandler());
    }

    public static ToolDefinition getTaskUpdateDefinition() {
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("type", "object");

        Map<String, Object> taskProps = new HashMap<>();

        Map<String, Object> taskId = new HashMap<>();
        taskId.put("type", "integer");
        taskId.put("description", "任务ID");
        taskProps.put("taskId", taskId);

        Map<String, Object> status = new HashMap<>();
        status.put("type", "string");
        status.put("description", "任务状态，可选值为 pending in_progress completed");
        taskProps.put("status", status);

        Map<String, Object> addBlockedBy = new HashMap<>();
        addBlockedBy.put("type", "array");
        addBlockedBy.put("items", Map.of("type", "integer"));
        addBlockedBy.put("description", "将指定的任务ID加入当前任务的前置依赖列表，任务必须等这些前置任务完成后才能开始");
        taskProps.put("addBlockedBy", addBlockedBy);

        Map<String, Object> removeBlockedBy = new HashMap<>();
        removeBlockedBy.put("type", "array");
        removeBlockedBy.put("items", Map.of("type", "integer"));
        removeBlockedBy.put("description", "从当前任务的前置依赖列表中移除指定的任务ID，移除后该前置任务的约束不再生效");
        taskProps.put("removeBlockedBy", removeBlockedBy);

        taskParams.put("properties", taskProps);
        taskParams.put("required", List.of("taskId"));

        return new ToolDefinition("task_update", "任务更新", taskParams, new TaskHandler());
    }

    public static ToolDefinition getTaskListDefinition() {
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("type", "object");

        Map<String, Object> taskProps = new HashMap<>();
        taskParams.put("properties", taskProps);
        taskParams.put("required", new ArrayList<>());

        return new ToolDefinition("task_list", "任务列表", taskParams, new TaskHandler());
    }

    public static ToolDefinition getTaskDetailDefinition() {
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("type", "object");

        Map<String, Object> taskProps = new HashMap<>();

        Map<String, Object> taskId = new HashMap<>();
        taskId.put("type", "integer");
        taskId.put("description", "任务ID");
        taskProps.put("taskId", taskId);

        taskParams.put("properties", taskProps);
        taskParams.put("required", List.of("taskId"));

        return new ToolDefinition("task_detail", "获取指定任务的详细信息", taskParams, new TaskHandler());
    }

    private static ToolDefinition getBackgroundCheckDefinition() {
        Map<String, Object> backgroundCheckParams = new HashMap<>();
        backgroundCheckParams.put("type", "object");

        Map<String, Object> backgroundCheckProps = new HashMap<>();
        Map<String, Object> jobId = new HashMap<>();
        jobId.put("type", "string");
        jobId.put("description", "需要查询的后台任务ID");
        backgroundCheckProps.put("jobId", jobId);

        backgroundCheckParams.put("properties", backgroundCheckProps);
        backgroundCheckParams.put("required", List.of("jobId"));

        return new ToolDefinition("background_check", "获取指定后台任务的状态", backgroundCheckParams, new BackgroundHandler());
    }

    private static ToolDefinition getBackgroundSubmitDefinition() {
        Map<String, Object> backgroundSubmitParams = new HashMap<>();
        backgroundSubmitParams.put("type", "object");

        Map<String, Object> backgroundSubmitProps = new HashMap<>();
        Map<String, Object> type = new HashMap<>();
        type.put("type", "string");
        type.put("description", "需要提交到后台任务的类型，目前只支持subAgent");
        backgroundSubmitProps.put("type", type);

        Map<String, Object> description = new HashMap<>();
        description.put("type", "string");
        description.put("description", "需要提交的后台任务详细描述");
        backgroundSubmitProps.put("description", description);

        Map<String, Object> prompt = new HashMap<>();
        prompt.put("type", "string");
        prompt.put("description", "需要后台SubAgent研究的内容，如果type为subAgent，这个参数必须传");
        backgroundSubmitProps.put("prompt", prompt);

        backgroundSubmitParams.put("properties", backgroundSubmitProps);
        backgroundSubmitParams.put("required", List.of("type"));

        return new ToolDefinition("background_submit", "提交后台任务的状态", backgroundSubmitParams, new BackgroundHandler());
    }

    private static Map<String, Object> teamTool(String name, String description,
                                                Map<String, Object> properties) {
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        Map<String, Object> params = new HashMap<>();
        params.put("type", "object");
        params.put("properties", properties);
        tool.put("parameters", params);
        return tool;
    }

    public static ToolDefinition getSpawnTeammateDefinition() {
        Map<String, Object> spawnTeammateParams;
        spawnTeammateParams = teamTool("spawn_teammate", "启动一个持久化的队友Agent",
                Map.of(
                        "name", Map.of("type", "string", "description", "队友名字"),
                        "role", Map.of("type", "string", "description", "队友角色，如 coder、reviewer"),
                        "prompt", Map.of("type", "string", "description", "给队友的初始任务")
                ));
        return new ToolDefinition("spawn_teammate", "启动一个持久化的队友Agent", spawnTeammateParams, new SpawnTeammateHandler());
    }

    public static ToolDefinition getListTeammatesDefinition() {
        Map<String, Object> listTeammatesParams;
        listTeammatesParams = teamTool("list_teammates", "列出所有队友的状态",
                Map.of());
        return new ToolDefinition("list_teammates", "列出所有队友的状态", listTeammatesParams, new ListTeammateHandler());
    }

    public static ToolDefinition getReadInboxDefinition() {
        Map<String, Object> readInboxParams;
        readInboxParams = teamTool("read_inbox", "读取并清空自己的收件箱", Map.of(
                "name", Map.of("type", "string", "description", "收件人的姓名")
        ));
        return new ToolDefinition("read_inbox", "读取并清空自己的收件箱", readInboxParams, new ReadInboxHandler());
    }

    public static ToolDefinition getSendMessageDefinition() {
        Map<String, Object> sendMessageParams;
        sendMessageParams = teamTool("send_message", "给队友发消息",
                Map.of(
                        "sender", Map.of("type", "string", "description", "发送者名字"),
                        "to", Map.of("type", "string", "description", "队友名字"),
                        "content", Map.of("type", "string", "description", "消息内容")
                ));
        return new ToolDefinition("send_message", "给队友发消息", sendMessageParams, new SendMessageHandler());
    }

    public static ToolDefinition getBroadcastDefinition() {
        Map<String, Object> broadcastParams;
        broadcastParams = teamTool("broadcast", "给所有队友广播消息",
                Map.of(
                        "sender", Map.of("type", "string", "description", "发送者名字"),
                        "content", Map.of("type", "string", "description", "广播内容")));
        return new ToolDefinition("broadcast", "给所有队友广播消息", broadcastParams, new BroadcastHandler());
    }


}
