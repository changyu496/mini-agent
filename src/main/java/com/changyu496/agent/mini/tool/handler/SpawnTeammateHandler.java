package com.changyu496.agent.mini.tool.handler;

import com.changyu496.agent.mini.team.TeammateManger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Map;

public class SpawnTeammateHandler implements ToolHandler{
    @Override
    public String execute(String argJson, String functionName) {
        try {
            Map<String, String> args = new ObjectMapper().readValue(argJson, Map.class);
            String name = args.get("name");
            String role = args.get("role");
            String prompt = args.get("prompt");
            return TeammateManger.getInstance(Path.of(System.getProperty("user.home") + "/.team")).spawn(name, role, prompt);
        } catch (Exception e) {
            return "启动队友失败: " + e.getMessage();
        }
    }
}
