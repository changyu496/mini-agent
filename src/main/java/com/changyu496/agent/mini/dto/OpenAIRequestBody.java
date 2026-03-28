package com.changyu496.agent.mini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OpenAIRequestBody {

    private String model;

    private List<Message> messages;
    private Double temperature;

    private List<Map<String,Object>> tools;

    @JsonProperty("extra_body")
    private Map<String,Object> extraBody;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public List<Map<String,Object>> getTools() {
        return tools;
    }

    public void setTools(List<Map<String,Object>> tools) {
        this.tools = tools;
    }

    public Map<String, Object> getExtraBody() {
        return extraBody;
    }

    public void setExtraBody(Map<String, Object> extraBody) {
        this.extraBody = extraBody;
    }
}
