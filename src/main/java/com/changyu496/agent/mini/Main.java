package com.changyu496.agent.mini;

import com.changyu496.agent.mini.dto.Message;
import com.changyu496.agent.mini.dto.OpenAIResponse;
import com.changyu496.agent.mini.dto.ToolCall;
import com.changyu496.agent.mini.tool.*;

import java.util.*;

public class Main {

    private static final Map<String, ToolDefinition> dispatcher = new HashMap<>();

    static {
        dispatcher.put("read_file", getReadFileDefinition());
        dispatcher.put("write_file", getWriteFileDefinition());
        dispatcher.put("search_notes", getSearchNotesDefinition());
        dispatcher.put("todo", getTodoDefinition());
    }

    private static final int MAX_MESSAGE_SIZE = 100;

    public static void main(String[] args) {
        List<Message> historyMessages = new ArrayList<>();

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
                    OpenAIResponse openAIResponse = client.call(historyMessages, regTool());
                    assistantMsg = openAIResponse.getChoices().get(0).getMessage();
                    // 过滤掉 <排除think> 和 </排除think> 标签
                    String filtered = assistantMsg.getContent()
                            .replaceAll("<think>[\\s\\S]*?</think>", "")
                            .trim();
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
                            toolResult.setToolCallId(toolCall.getId());
                            toolResult.setContent(result);
                            historyMessages.add(toolResult);
                        }
                    }
                } while ("tool_calls".equals(finishReason));
                System.out.println("assistant:" + assistantMsg.getContent());
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

    private static void buildSystemPrompt(List<Message> historyMessages) {
        Message system = new Message();
        system.setRole("system");
        String todoStatus = TodoManager.getInstance().render();
        system.setContent("你是一个读书伴侣，专注于帮助用户深入理解读过的书。\n" +
                "你的核心能力：\n" +
                "- 读取用户的读书笔记（使用 read_file 工具）\n" +
                "- 写入和更新笔记（使用 write_file 工具）\n" +
                "- 管理待办和进度（使用 todo 工具）\n" +
                "- 基于笔记内容展开讨论和追问\n" +
                todoStatus + "\n" +
                "【关键规则】\n" +
                "- 用户提到更新读书进度（读到哪章、换书等）→ 必须调用 todo 工具，action=progress\n" +
                "- 用户提到添加待办、完成任务 → 必须调用 todo 工具，action=update\n" +
                "- 其他情况（读笔记、写笔记）才用 read_file / write_file\n" +
                "笔记目录：~/.reading-agent/workspace/\n" +
                "todo 文件：~/.reading-agent/workspace/todo.json");
        historyMessages.add(system);
    }

    private static List<Map<String, Object>> regTool() {
        List<Map<String, Object>> tools = new ArrayList<>();
        dispatcher.values().forEach(toolDefinition -> {
            Map<String, Object> toolMap = new HashMap<>();
            Map<String, Object> function = new HashMap<>();
            toolMap.put("function", function);
            toolMap.put("type", "function");
            function.put("name", toolDefinition.getName());
            function.put("description", toolDefinition.getDescription());
            function.put("parameters", toolDefinition.getParameterSchema());
            tools.add(toolMap);
        });
        return tools;
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
        return new ToolDefinition("read_file",
                "读取文件内容",
                readParams, new ReadFileHandler()
        );
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

        Map<String, Object> progress = new HashMap<>();
        progress.put("type", "string");
        progress.put("description", "当前进度，仅 action = progress时用");
        todoProps.put("progress", progress);

        todoParams.put("properties", todoProps);
        todoParams.put("required", List.of("action"));

        return new ToolDefinition("todo", "管理读书进度和待办事项", todoParams, new TodoHandler());
    }
}
