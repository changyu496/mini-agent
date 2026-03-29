package com.changyu496.agent.mini;

import com.changyu496.agent.mini.dto.Message;
import com.changyu496.agent.mini.dto.OpenAIResponse;
import com.changyu496.agent.mini.dto.ToolCall;

import java.util.*;

public class Main {


    private static final int MAX_MESSAGE_SIZE = 100;

    public static void main(String[] args) {

        List<Message> historyMessages = new ArrayList<>();
        Message system = new Message();
        system.setRole("system");
        system.setContent("你是一个mini助手，帮助用户解决问题");
        historyMessages.add(system);
        Scanner scanner = new Scanner(System.in);
        OpenAIHttpClient client = new OpenAIHttpClient();
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
                do{
                    OpenAIResponse openAIResponse = client.call(historyMessages);
                    Message assistantMsg = openAIResponse.getChoices().get(0).getMessage();
                    finishReason = openAIResponse.getChoices().get(0).getFinishReason();
                    historyMessages.add(assistantMsg);
                    if ("tool_calls".equals(finishReason)){
                        for(ToolCall toolCall : assistantMsg.getToolCalls()){
                            String result = callWeather(toolCall);
                            Message toolResult = new Message();
                            toolResult.setRole("tool");
                            toolResult.setToolCallId(toolCall.getId());
                            toolResult.setContent(result);
                            historyMessages.add(toolResult);
                        }
                    }
                }while ("tool_calls".equals(finishReason));
                Message finalMsg = historyMessages.get(historyMessages.size()-1);
                System.out.println("assistant:" + finalMsg.getContent());
            } catch (Exception e) {
                System.out.println("大模型调用异常，请稍后重试");
            }
        }
    }

    private static String callWeather(ToolCall toolCall){
        return "今天的天气晴，29度";
    }

}
