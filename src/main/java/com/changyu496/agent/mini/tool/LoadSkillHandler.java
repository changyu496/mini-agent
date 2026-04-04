package com.changyu496.agent.mini.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class LoadSkillHandler implements ToolHandler {

    @Override
    public String execute(String argJson) {

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map argMap = objectMapper.readValue(argJson, Map.class);
            return SkillLoader.getInstance().getContent((String) argMap.get("name"));
        } catch (JsonProcessingException e) {
            return "加载Skill遇到异常" + e.getMessage();
        }
    }
}
