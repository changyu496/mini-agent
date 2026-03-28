package com.changyu496.agent.mini;

import com.changyu496.agent.mini.dto.Message;
import com.changyu496.agent.mini.dto.OpenAIRequestBody;
import com.changyu496.agent.mini.dto.OpenAIResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OpenAIHttpClient {
    private static final String apiKey = System.getenv("MINI_AGENT_API_KEY");
    private static String url = "https://api.minimaxi.com/v1/chat/completions";

    private OkHttpClient client;

    public OpenAIHttpClient(){
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60,TimeUnit.SECONDS)
                .writeTimeout(60,TimeUnit.SECONDS)
                .callTimeout(90,TimeUnit.SECONDS)
                .build();
    }
    public OpenAIResponse call(List<Message> messages){
        OpenAIResponse openAIResponse;
        OpenAIRequestBody openAIRequestBody = new OpenAIRequestBody();
        openAIRequestBody.setModel("MiniMax-M2.7");
        openAIRequestBody.setTemperature(0.7);
        openAIRequestBody.setMessages(messages);
        openAIRequestBody.setTools(getTools());
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

    private List<Map<String,Object>> getTools(){
        List<Map<String,Object>> tools = new ArrayList<>();
        Map<String,Object> weatherTool = new HashMap<>();
        weatherTool.put("type","function");
        Map<String,Object> function = new HashMap<>();
        weatherTool.put("function",function);
        function.put("name","get_weather");
        function.put("description","Get weather of a location, the user should supply a location first.");
        Map<String,Object> parameters = new HashMap<>();
        function.put("parameters",parameters);
        parameters.put("type","object");
        Map<String,Object> properties = new HashMap<>();
        parameters.put("properties",properties);
        Map<String,Object> location = new HashMap<>();
        properties.put("location",location);
        location.put("type","string");
        location.put("description","The city and state, e.g. San Francisco, US");
        List<String> requiredDef = new ArrayList<>();
        requiredDef.add("location");
        parameters.put("required",requiredDef);
        tools.add(weatherTool);
        return tools;
    }

}
