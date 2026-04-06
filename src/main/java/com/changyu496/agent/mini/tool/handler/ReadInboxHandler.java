package com.changyu496.agent.mini.tool.handler;

import com.changyu496.agent.mini.team.TeammateManger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ReadInboxHandler implements ToolHandler {
    @Override
    public String execute(String argJson, String functionName) {
        try {
            Map<String, String> args = new ObjectMapper().readValue(argJson, Map.class);
            String name = args.get("name");
            TeammateManger instance = TeammateManger.getInstance(Path.of(System.getProperty("user.home") + "/.team"));
            List<Map<String, Object>> list = instance.getMessageBus().readInbox(name);
            StringBuilder stringBuilder = new StringBuilder();
            for (Map<String, Object> stringObjectMap : list) {
                stringBuilder.append(stringObjectMap.get("content") + "\n");
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            return "读取收件箱遇到异常";
        }
    }
}
