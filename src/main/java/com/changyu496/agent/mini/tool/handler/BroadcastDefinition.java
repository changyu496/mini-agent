package com.changyu496.agent.mini.tool.handler;

import com.changyu496.agent.mini.team.TeammateManger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Map;

public class BroadcastDefinition implements ToolHandler {

    @Override
    public String execute(String argJson, String functionName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> argMap = objectMapper.readValue(argJson, Map.class);
            TeammateManger instance = TeammateManger.getInstance(Path.of(System.getProperty("user.home") + "./team"));
            String sender = String.valueOf(argMap.get("sender"));
            String content = String.valueOf(argMap.get("content"));
            return instance.getMessageBus().broadcast(sender, content, instance.memberNames());
        } catch (Exception e) {
            return "广播消息遇到异常" + e.getMessage();
        }
    }
}
