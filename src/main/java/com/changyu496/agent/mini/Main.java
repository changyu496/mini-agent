package com.changyu496.agent.mini;

import com.changyu496.agent.mini.dto.Message;
import com.changyu496.agent.mini.dto.OpenAIResponse;
import com.changyu496.agent.mini.dto.ToolCall;
import com.changyu496.agent.mini.tool.ReadFileHandler;
import com.changyu496.agent.mini.tool.ToolDefinition;
import com.changyu496.agent.mini.tool.WriteFileHandler;

import java.util.*;

public class Main {

    private static final Map<String, ToolDefinition> dispatcher = new HashMap<>();

    static {
        dispatcher.put("read_file", getReadFileDefinition());
        dispatcher.put("write_file", getWriteFileDefinition());
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
                System.out.println("assistant:" + assistantMsg.getContent());
            } catch (Exception e) {
                System.out.println("大模型调用异常，请稍后重试");
            }
        }
    }

    private static void buildSystemPrompt(List<Message> historyMessages) {
        Message system = new Message();
        system.setRole("system");
        system.setContent("你是一个mini助手，帮助用户解决问题");
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
}
