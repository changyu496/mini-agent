package com.changyu496.agent.mini.tool.handler;

import com.changyu496.agent.mini.team.MessageBus;
import com.changyu496.agent.mini.team.TeammateManger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PlanApproveHandler implements ToolHandler {

    @Override
    public String execute(String argJson, String functionName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> map = objectMapper.readValue(argJson, Map.class);
            String requestId = String.valueOf(map.get("request_id"));
            Boolean approve = Boolean.parseBoolean(String.valueOf(map.get("approve")));
            String feedback = String.valueOf(map.get("feedback"));
            TeammateManger instance = TeammateManger.getInstance(Path.of(System.getProperty("user.home") + "/.team"));

            Map<String, Object> req = TeammateManger.getPlanRequests().get(requestId);
            if (req == null) {
                return "未找到 request_id" + requestId;
            }

            req.put("status", approve ? "approved" : "rejected");

            String toTeammate = String.valueOf(req.get("from"));
            Map<String, Object> extra = new HashMap<>();
            extra.put("request_id", requestId);
            extra.put("approve", approve);
            extra.put("feedback", feedback);
            MessageBus messageBus = instance.getMessageBus();
            messageBus.send("lead", toTeammate, feedback, "plan_approval_response", extra);

            return "计划已" + (approve ? "批准" : "拒绝") + " (id=" + requestId + ")";
        } catch (Exception e) {
            return "PlanApprovalHandler 执行失败: " + e.getMessage();
        }
    }
}

