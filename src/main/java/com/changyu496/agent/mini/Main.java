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


    public static void main(String[] args) {

        List<Message> historyMessages = new ArrayList<>();

        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("mini agent >> ");
            String userInput = scanner.nextLine();
            if ("exit".equals(userInput) || "q".equals(userInput)){
                break;
            }
            OpenAIHttpClient client = new OpenAIHttpClient();
            Message message = new Message();
            message.setContent(userInput);
            message.setRole("user");
            historyMessages.add(message);
            OpenAIResponse openAIResponse = client.call(historyMessages);
            historyMessages.add(openAIResponse.getChoices().get(0).getMessage());
            System.out.println("assistant:" + openAIResponse.getChoices().get(0).getMessage().getContent());
        }


    }

}
