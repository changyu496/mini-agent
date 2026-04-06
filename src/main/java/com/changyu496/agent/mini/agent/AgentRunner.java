package com.changyu496.agent.mini.agent;

import com.changyu496.agent.mini.OpenAIHttpClient;
import com.changyu496.agent.mini.dto.Message;
import com.changyu496.agent.mini.dto.OpenAIResponse;
import com.changyu496.agent.mini.dto.ToolCall;

import java.util.List;
import java.util.Map;

public class AgentRunner {

    @FunctionalInterface
    public interface ToolExecutor {
        String execute(String toolName, String argsJson);
    }

    @FunctionalInterface
    public interface NotificationHandler {
        void handle(List<Message> messages);
    }

    public static String runForever(OpenAIHttpClient client, List<Message> messages, List<Map<String, Object>> tools, ToolExecutor executor, NotificationHandler notificationHandler) {
        return run(client, messages, tools, executor, notificationHandler, -1);
    }

    public static String run(OpenAIHttpClient client, List<Message> messages, List<Map<String, Object>> tools, ToolExecutor executor, NotificationHandler notificationHandler, int maxRound) {
        int round = 0;
        while (true) {
            if (notificationHandler != null) {
                notificationHandler.handle(messages);
            }
            if (maxRound < 0) {

            } else if (round >= maxRound) {
                return "达到最大轮次限制";
            }
            OpenAIResponse resp = client.call(messages, tools);
            Message assistantMsg = resp.getChoices().get(0).getMessage();
            messages.add(assistantMsg);
            // 过滤掉 <排除think> 和 </排除think> 标签
            String filtered = assistantMsg.getContent().replaceAll("<think>[\\s\\S]*?</think>", "").trim();
            assistantMsg.setContent(filtered);
            String finishReason = resp.getChoices().get(0).getFinishReason();
            if (!"tool_calls".equals(finishReason)) {
                return assistantMsg.getContent();
            }

            // 执行工具
            for (ToolCall tc : assistantMsg.getToolCalls()) {
                String result = executor.execute(tc.getFunction().getName(),
                        tc.getFunction().getArguments());
                messages.add(toolResult(tc.getId(), tc.getFunction().getName(), result));
            }
            round++;
            microCompact(messages);
        }
    }

    private static Message toolResult(String toolCallId, String toolCallName, String result) {
        Message toolResult = new Message();
        toolResult.setRole("tool");
        toolResult.setToolCallId(toolCallId);
        toolResult.setName(toolCallName);
        toolResult.setContent(result);
        return toolResult;
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
}
