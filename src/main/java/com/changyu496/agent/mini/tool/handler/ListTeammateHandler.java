package com.changyu496.agent.mini.tool.handler;

import com.changyu496.agent.mini.team.TeammateManger;

import java.nio.file.Path;

public class ListTeammateHandler implements ToolHandler {
    @Override
    public String execute(String argJson, String functionName) {
        try {
            return TeammateManger.getInstance(Path.of(System.getProperty("user.home") + "/.team")).listAll();
        } catch (Exception e) {
            return "获取队友列表失败: " + e.getMessage();
        }
    }
}
