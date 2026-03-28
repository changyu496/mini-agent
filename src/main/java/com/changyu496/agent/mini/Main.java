package com.changyu496.agent.mini;

import com.changyu496.agent.mini.dto.Message;
import com.changyu496.agent.mini.dto.OpenAIRequestBody;
import com.changyu496.agent.mini.dto.OpenAIResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {


    private static final int MAX_MESSAGE_SIZE = 20;

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
            if ("exit".equals(userInput) || "q".equals(userInput)){
                break;
            }
            Message message = new Message();
            message.setContent(userInput);
            message.setRole("user");
            historyMessages.add(message);
            if (historyMessages.size()>=MAX_MESSAGE_SIZE){
                // 永远保留第一个system prompt
                historyMessages.remove(1);
            }
            try {
                OpenAIResponse openAIResponse = client.call(historyMessages);
                historyMessages.add(openAIResponse.getChoices().get(0).getMessage());
                System.out.println("assistant:" + openAIResponse.getChoices().get(0).getMessage().getContent());
            }catch (Exception e){
                System.out.println("大模型调用异常，请稍后重试");
            }

        }


    }

}
