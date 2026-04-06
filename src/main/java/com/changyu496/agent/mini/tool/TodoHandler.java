package com.changyu496.agent.mini.tool;

import com.changyu496.agent.mini.todo.TodoItem;
import com.changyu496.agent.mini.tool.handler.ToolHandler;
import com.changyu496.agent.mini.todo.TodoManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class TodoHandler implements ToolHandler {

    @Override
    public String execute(String argJson, String functionName) {
        ObjectMapper objectMapper = new ObjectMapper();
        TodoManager todoManager = TodoManager.getInstance();
        try {
            Map argMap = objectMapper.readValue(argJson, Map.class);
            String action = String.valueOf(argMap.get("action"));
            if ("update".equals(action)) {
                List<Map<String, String>> itemsRaw = (List<Map<String, String>>) argMap.get("items");
                List<TodoItem> todoItems = itemsRaw.stream().map(m -> {
                    TodoItem item = new TodoItem();
                    item.setId(m.get("id"));
                    item.setText(m.get("text"));
                    item.setStatus(m.getOrDefault("status", "pending"));
                    return item;
                }).toList();

                todoManager.updateItems(todoItems);
                return todoManager.render();
            }
            if ("progress".equals(action)) {
                todoManager.updateProgress((String) argMap.get("book"), (Integer) argMap.get("chapter"), (String) argMap.get("progress"));
                return todoManager.render();
            }
        } catch (JsonProcessingException e) {
            return "解析TODO工具的参数遇到异常" + e.getMessage();
        }
        return "参数非法";
    }
}
