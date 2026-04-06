package com.changyu496.agent.mini.tool.handler;

import com.changyu496.agent.mini.team.MessageBus;
import com.changyu496.agent.mini.team.TeammateManger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShutdownRequestHandler implements ToolHandler {
    @Override
    public String execute(String argJson, String functionName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> map = objectMapper.readValue(argJson, Map.class);
            String teammate = String.valueOf(map.get("teammate"));
            TeammateManger instance = TeammateManger.getInstance(Path.of(System.getProperty("user.home") + "/.team"));

            String requestId = UUID.randomUUID().toString().toString().substring(0, 8);

            Map<String, Object> req = new HashMap<>();
            req.put("target", teammate);
            req.put("status", "pending");
            TeammateManger.getShutdownRequests().put(requestId, req);

            Map<String, Object> extra = new HashMap<>();
            extra.put("request_id", requestId);
            MessageBus messageBus = instance.getMessageBus();
            messageBus.send("lead", teammate, "请优雅关闭", "shutdown_request", extra);

            return "Shutdown request 已经发送给" + teammate + "request_id:" + requestId;

        } catch (Exception e) {
            return "解析参数遇到异常" + e.getMessage();
        }
    }
}
