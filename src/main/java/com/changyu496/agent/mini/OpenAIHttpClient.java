package com.changyu496.agent.mini;

import com.changyu496.agent.mini.dto.Message;
import com.changyu496.agent.mini.dto.OpenAIRequestBody;
import com.changyu496.agent.mini.dto.OpenAIResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OpenAIHttpClient {
    private static String apiKey = "sk-cp-oGj492tV56TQ7Gtnci-V-KSbPNKvGu3nL7TYHUh3AVahtH9SlIUkmX9U2rIj7lu2pwabVqZxKmwsftEGcwA_4rpF1zNqrrRa57mwU6g6DMPFt55hEqvOaOI";
    private static String url = "https://api.minimaxi.com/v1/chat/completions";

    private OkHttpClient client;

    public OpenAIHttpClient(){
        client = new OkHttpClient();
    }
    public OpenAIResponse call(List<Message> messages){
        OpenAIResponse openAIResponse;
        OpenAIRequestBody openAIRequestBody = new OpenAIRequestBody();
        openAIRequestBody.setModel("MiniMax-M2.7");
        openAIRequestBody.setTemperature(0.7);
        openAIRequestBody.setMessages(messages);
        ObjectMapper requestMapper = new ObjectMapper();
        String openAIRequestBodyJsonStr = "";
        try {
            openAIRequestBodyJsonStr = requestMapper.writeValueAsString(openAIRequestBody);
        }catch (Exception e){
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(openAIRequestBodyJsonStr, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization","Bearer "+apiKey)
                .post(body)
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()){
                throw new IOException("Unexpected code " + response);
            }
            ObjectMapper responseMapper = new ObjectMapper();
            openAIResponse = responseMapper.readValue(response.body().string(), OpenAIResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return openAIResponse;
    }

}
