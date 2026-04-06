package com.changyu496.agent.mini.tool.handler;

import com.changyu496.agent.mini.background.BackgroundManger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class BackgroundHandler implements ToolHandler {
    @Override
    public String execute(String argJson, String functionName) {
        Map<String, Object> argMap;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            argMap = objectMapper.readValue(argJson, Map.class);
            if ("background_submit".equals(functionName)) {
                String type = String.valueOf(argMap.get("type"));
                String description = String.valueOf(argMap.get("description"));
                String prompt = "";
                if ("subAgent".equals(type)) {
                    prompt = String.valueOf(argMap.get("prompt"));
                }
                String finalPrompt = prompt;
                String jobId = BackgroundManger.getInstance().submit(type, description, () -> {
                    // 现在只有subAgent支持后台
                    return SubAgentHandler.getInstance().runWithResult(finalPrompt);
                });
                return "后台任务已启动，jobId为:" + jobId;
            }
            if ("background_check".equals(functionName)) {
                String jobId = String.valueOf(argMap.get("jobId"));
                return BackgroundManger.getInstance().check(jobId);
            }
        } catch (JsonProcessingException e) {
            return "后台任务的参数解析异常" + e.getMessage();
        }
        return "不支持的操作，请检查";
    }
}
