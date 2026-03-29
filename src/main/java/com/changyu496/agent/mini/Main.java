package com.changyu496.agent.mini;

import com.changyu496.agent.mini.dto.Message;
import com.changyu496.agent.mini.dto.OpenAIResponse;
import com.changyu496.agent.mini.dto.ToolCall;
import com.changyu496.agent.mini.tool.ReadFileHandler;
import com.changyu496.agent.mini.tool.SearchNotesHandler;
import com.changyu496.agent.mini.tool.ToolDefinition;
import com.changyu496.agent.mini.tool.WriteFileHandler;

import java.util.*;

public class Main {

    private static final Map<String, ToolDefinition> dispatcher = new HashMap<>();

    static {
        dispatcher.put("read_file", getReadFileDefinition());
        dispatcher.put("write_file", getWriteFileDefinition());
        dispatcher.put("search_notes", getSearchNotesDefinition());
    }

    private static final int MAX_MESSAGE_SIZE = 100;

    public static void main(String[] args) {
        List<Message> historyMessages = new ArrayList<>();

        Scanner scanner = new Scanner(System.in);
        OpenAIHttpClient client = new OpenAIHttpClient();
        buildSystemPrompt(historyMessages);
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
                    finishReason = openAIResponse.getChoices().get(0).getFinishReason();
                    historyMessages.add(assistantMsg);
                    if ("tool_calls".equals(finishReason)) {
                        for (ToolCall toolCall : assistantMsg.getToolCalls()) {
                            String result = callTool(toolCall);
                            Message toolResult = new Message();
                            toolResult.setRole("tool");
                            toolResult.setToolCallId(toolCall.getId());
                            toolResult.setContent(result);
                            historyMessages.add(toolResult);
                        }
                    }
                } while ("tool_calls".equals(finishReason));
                String content = assistantMsg.getContent();
                // 过滤掉 <排除think> 和 </排除think> 标签
                content = content.replaceAll("<think>[\\s\\S]*?</think>", "").trim();
                System.out.println("assistant:" + content);
            } catch (Exception e) {
                System.out.println("大模型调用异常，请稍后重试");
            }
        }
    }

    private static void buildSystemPrompt(List<Message> historyMessages) {
        Message system = new Message();
        system.setRole("system");
        system.setContent("你是一个读书伴侣，专注于帮助用户深入理解读过的书。\n" +
                "你的核心能力：\n" +
                "- 读取用户的读书笔记（使用 read_file 工具）\n" +
                "- 写入和更新笔记（使用 write_file 工具）\n" +
                "- 基于笔记内容展开讨论和追问\n" +
                "重要原则：\n" +
                "1. 在调用任何工具之前，先在回复中说明你的计划，格式如下：\n" +
                "   计划：[你要做的事情，简要说明]\n" +
                "2. 等待用户确认后，再执行工具调用\n" +
                "3. 每次讨论后，主动询问用户是否要将重要内容写入笔记\n" +
                "笔记目录：~/.reading-agent/workspace/");
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
}
