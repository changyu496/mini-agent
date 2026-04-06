package com.changyu496.agent.mini.tool.handler;

import com.changyu496.agent.mini.Main;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class SubAgentHandler implements ToolHandler {

    private static final SubAgentHandler instance = new SubAgentHandler();

    public static SubAgentHandler getInstance() {
        return instance;
    }

    private SubAgentHandler() {

    }

    @Override
    public String execute(String argJson, String functionName) {
        ObjectMapper argMapper = new ObjectMapper();
        try {
            Map<String, String> arg = argMapper.readValue(argJson, Map.class);
            String prompt = arg.get("prompt");
            return Main.runSubAgent(prompt);
        } catch (JsonProcessingException e) {
            return "创建子Agent遇到异常" + e.getMessage();
        }
    }

    public String runWithResult(String prompt) {
        return Main.runSubAgent(prompt);
    }
}
