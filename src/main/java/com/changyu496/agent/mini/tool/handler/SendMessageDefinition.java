package com.changyu496.agent.mini.tool.handler;

import com.changyu496.agent.mini.team.TeammateManger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Map;

public class SendMessageDefinition implements ToolHandler {

    @Override
    public String execute(String argJson, String functionName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> map = objectMapper.readValue(argJson, Map.class);
            TeammateManger instance = TeammateManger.getInstance(Path.of(System.getProperty("user.home") + ".team"));
            String sender = String.valueOf(map.get("sender"));
            String to = String.valueOf(map.get("to"));
            String content = String.valueOf(map.get("content"));
            // 这里默认都是单发
            instance.getMessageBus().send(sender, to, content, "single");
            return null;
        } catch (Exception e) {
            return "发送消息给队友遇到异常" + e.getMessage();
        }
    }
}
