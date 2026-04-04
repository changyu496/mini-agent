package com.changyu496.agent.mini;

import com.changyu496.agent.mini.dto.Message;
import com.changyu496.agent.mini.dto.OpenAIResponse;
import com.changyu496.agent.mini.dto.ToolCall;
import com.changyu496.agent.mini.tool.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.nio.file.StandardOpenOption.APPEND;

public class Main {

    private static final Map<String, ToolDefinition> dispatcher = new HashMap<>();

    static {
        dispatcher.put("read_file", getReadFileDefinition());
        dispatcher.put("write_file", getWriteFileDefinition());
        dispatcher.put("search_notes", getSearchNotesDefinition());
        dispatcher.put("todo", getTodoDefinition());
        dispatcher.put("task", getTaskDefinition());
        dispatcher.put("load_skill", getLoadSkillDefinition());
        dispatcher.put("compact", getCompactDefinition());
    }

    private static final int MAX_MESSAGE_SIZE = 100;

    public static List<Message> historyMessages = new ArrayList<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        OpenAIHttpClient client = new OpenAIHttpClient();
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
                String finishReason;
                Message assistantMsg;

                do {
                    microCompact(historyMessages);
                    int estimatedTokens = historyMessages.stream().mapToInt(m -> m.getContent() == null ? 0 : m.getContent().length() / 4).sum();
                    if (estimatedTokens > 50000) {
                        historyMessages = autoCompact(historyMessages);
                    }
                    OpenAIResponse openAIResponse = client.call(historyMessages, regTool());
                    assistantMsg = openAIResponse.getChoices().get(0).getMessage();
                    // 过滤掉 <排除think> 和 </排除think> 标签
                    String filtered = assistantMsg.getContent().replaceAll("<think>[\\s\\S]*?</think>", "").trim();
                    assistantMsg.setContent(filtered);
                    finishReason = openAIResponse.getChoices().get(0).getFinishReason();
                    historyMessages.add(assistantMsg);
                    if ("tool_calls".equals(finishReason)) {
                        for (ToolCall toolCall : assistantMsg.getToolCalls()) {
                            String result = callTool(toolCall);
                            if ("todo".equals(toolCall.getFunction().getName())) {
                                roundSinceTodo = 0;
                            }
                            Message toolResult = new Message();
                            toolResult.setRole("tool");
                            toolResult.setName(toolCall.getFunction().getName());
                            toolResult.setToolCallId(toolCall.getId());
                            toolResult.setContent(result);
                            historyMessages.add(toolResult);
                        }
                    }
                } while ("tool_calls".equals(finishReason));
                System.out.println("assistant:" + assistantMsg.getContent());
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

    private static void microCompact(List<Message> historyMessages) {
        int toolResultCount = 0;
        for (int i = historyMessages.size() - 1; i >= 0; i--) {
            Message msg = historyMessages.get(i);
            if (msg.getRole().equals("tool")) {
                toolResultCount++;
                if (toolResultCount > 3) {
                    System.out.println("[microCompact] 替换: " + msg.getName()
                            + " -> " + msg.getContent().substring(0, Math.min(50, msg.getContent().length())) + "...");

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
        OpenAIHttpClient openAIHttpClient = new OpenAIHttpClient();
        Message system = new Message();
        system.setRole("system");
        system.setContent("请根据内容帮我最摘要，返回[摘要信息]+[确认消息]");
        List<Message> summaryMessage = new ArrayList<>();
        Message user = new Message();
        user.setRole("user");
        user.setContent(stringBuilder.toString());
        summaryMessage.add(system);
        summaryMessage.add(user);
        OpenAIResponse openAIResponse = openAIHttpClient.call(summaryMessage, new ArrayList<>());
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

        // 最大轮数
        int maxRound = 30;
        int round = 0;

        while (round < maxRound) {
            OpenAIHttpClient openAIHttpClient = new OpenAIHttpClient();
            List<Map<String, Object>> tools = regBasicTool();

            OpenAIResponse openAIResponse = openAIHttpClient.call(subMessages, tools);
            Message assistantMessage = openAIResponse.getChoices().get(0).getMessage();
            subMessages.add(assistantMessage);

            String finishReason = openAIResponse.getChoices().get(0).getFinishReason();

            if (!"tool_calls".equals(finishReason)) {
                return assistantMessage.getContent() != null ? assistantMessage.getContent() : "(无结果)";
            }
            for (ToolCall toolCall : assistantMessage.getToolCalls()) {
                String toolResult = callTool(toolCall);
                Message toolResultMessage = new Message();
                toolResultMessage.setContent(toolResult);
                toolResultMessage.setName(toolCall.getFunction().getName());
                toolResultMessage.setRole("tool");
                toolResultMessage.setToolCallId(toolCall.getId());
                subMessages.add(toolResultMessage);
            }
            round++;
        }
        return "(达到最大轮次限制)";
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
        tools.add(toolToMap(getTaskDefinition()));
        tools.add(toolToMap(getLoadSkillDefinition()));
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

    private static Map<String, Object> toolToMap(ToolDefinition toolDefinition) {
        Map<String, Object> toolMap = new HashMap<>();
        Map<String, Object> function = new HashMap<>();
        toolMap.put("function", function);
        toolMap.put("type", "function");
        function.put("name", toolDefinition.getName());
        function.put("description", toolDefinition.getDescription());
        function.put("parameters", toolDefinition.getParameterSchema());
        return toolMap;
    }

    private static String callTool(ToolCall toolCall) {
        String functionName = toolCall.getFunction().getName();
        String arguments = toolCall.getFunction().getArguments();
        ToolDefinition toolDefinition = dispatcher.get(functionName);
        if (Objects.isNull(toolDefinition)) {
            return "未知工具";
        }
        return toolDefinition.getToolHandler().execute(arguments);
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

    public static ToolDefinition getTaskDefinition() {
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("type", "object");
        Map<String, Object> taskProps = new HashMap<>();

        Map<String, Object> prompt = new HashMap<>();
        prompt.put("type", "string");
        prompt.put("description", "给子Agent的研究任务描述，要清晰具体");

        taskProps.put("prompt", prompt);

        taskParams.put("properties", taskProps);
        taskParams.put("required", List.of("prompt"));

        return new ToolDefinition("task", "启动一个子Agent，用新的上下文完成研究任务", taskParams, new TaskHandler());
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
}
