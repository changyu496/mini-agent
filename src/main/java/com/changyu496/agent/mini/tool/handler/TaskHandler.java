package com.changyu496.agent.mini.tool.handler;

import com.changyu496.agent.mini.task.Task;
import com.changyu496.agent.mini.task.TaskManger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskHandler implements ToolHandler {

    @Override
    public String execute(String argJson, String functionName) {
        ObjectMapper objectMapper = new ObjectMapper();
        TaskManger instance = TaskManger.getInstance();
        try {
            Map map = objectMapper.readValue(argJson, Map.class);
            if (functionName.equals("task_create")) {
                String subject = String.valueOf(map.get("subject"));
                String description = String.valueOf(map.get("description"));
                instance.create(subject, description);
                return "新增成功";
            }
            if (functionName.equals("task_update")) {
                int taskId = Integer.parseInt(String.valueOf(map.get("taskId")));
                String status = String.valueOf(map.get("status"));
                // 从自己的里面移除
                List<Integer> addBlockedByRaw = (List<Integer>) map.get("addBlockedBy");
                List<Integer> removeBlockedByRaw = (List<Integer>) map.get("removeBlockedBy");
                List<Integer> addBlockedBy = addBlockedByRaw != null ? addBlockedByRaw : new ArrayList<>();
                List<Integer> removeBlockedBy = removeBlockedByRaw != null ? removeBlockedByRaw : new ArrayList<>();
                instance.update(taskId, status, addBlockedBy, removeBlockedBy);
                return "更新成功";
            }

            if (functionName.equals("task_list")) {
                List<Task> list = instance.list();
                StringBuilder allTask = new StringBuilder("当前任务状况如下：");
                list.forEach(t -> allTask.append(t.render()).append("\n"));
                return allTask.toString();

            }
            if (functionName.equals("task_detail")) {
                int taskId = Integer.parseInt((String) map.get("taskId"));
                return instance.getDetail(taskId).render();
            }
        } catch (JsonProcessingException e) {
            return "Task处理遇到异常" + e.getMessage();
        }

        return "未知的操作";
    }
}
