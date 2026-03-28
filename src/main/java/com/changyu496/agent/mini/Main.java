package com.changyu496.agent.mini;

import com.changyu496.agent.mini.dto.Message;
import com.changyu496.agent.mini.dto.OpenAIRequestBody;
import com.changyu496.agent.mini.dto.OpenAIResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {


    public static void main(String[] args) {
        OpenAIHttpClient client = new OpenAIHttpClient();
        Message message = new Message();
        message.setContent("Hi, how are you");
        message.setRole("user");
        List<Message> messages = new ArrayList<>();
        messages.add(message);
        OpenAIResponse openAIResponse = client.call(messages);
        System.out.println(openAIResponse.getChoices().get(0).getMessage());
    }

}
