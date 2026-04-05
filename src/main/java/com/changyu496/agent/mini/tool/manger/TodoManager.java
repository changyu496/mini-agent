package com.changyu496.agent.mini.tool.manger;

import com.changyu496.agent.mini.dto.TodoItem;
import com.changyu496.agent.mini.dto.TodoState;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Data
public class TodoManager {

    private static final TodoManager instance = new TodoManager();

    private TodoState todoState;

    public static TodoManager getInstance() {
        return instance;
    }

    private TodoManager() {
        read();
    }

    public void read() {
        // 读取json文件，返回格式化的状态
        TodoState todoState = null;
        try {
            String todoJson = Files.readString(Path.of(System.getProperty("user.home"), ".reading-agent", "todo.json"));
            ObjectMapper objectMapper = new ObjectMapper();
            todoState = objectMapper.readValue(todoJson, TodoState.class);
        } catch (IOException e) {
            //return "读取TODO文件遇到异常 " + e.getMessage();
        }
        this.todoState = todoState;
    }

    public String save(TodoState todoState) {
        Path todoPath = Path.of(System.getProperty("user.home"), ".reading-agent", "todo.json");
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String todoStateJson = objectMapper.writeValueAsString(todoState);
            Files.writeString(todoPath, todoStateJson);
        } catch (IOException e) {
            return "写入TODO文件遇到异常 " + e.getMessage();
        }
        return "写入成功";
    }

    public String updateItems(List<TodoItem> newItems) {
        todoState.setItems(newItems);

        long inProgressCount = newItems.stream().filter(todoItem -> "in_progress".equals(todoItem.getStatus())).count();
        if (inProgressCount > 1) {
            return "错误:同时只能有一个in_progress任务";
        }
        todoState.setLastUpdated(new Date());
        return save(todoState);
    }

    public String updateProgress(String book, Integer chapter, String progress) {
        if (Objects.nonNull(book) && !"".equals(book)) {
            todoState.setBook(book);
        }
        if (Objects.nonNull(chapter)) {
            todoState.setChapter(chapter);
        }
        if (Objects.nonNull(progress) && !"".equals(progress)) {
            todoState.setProgress(progress);
        }
        todoState.setLastUpdated(new Date());
        return save(todoState);
    }

    public String render() {
        if (Objects.isNull(todoState)) {
            return "暂无读书状态，请先调用 todo 设置当前书籍";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【当前状态】\n");
        if (todoState.getBook() != null) {
            sb.append("📖 ").append(todoState.getBook()).append("\n");
        }
        if (todoState.getChapterTitle() != null) {
            sb.append("第").append(todoState.getChapter()).append("章 - ")
                    .append(todoState.getChapterTitle()).append("\n");
        }
        if (todoState.getProgress() != null) {
            sb.append("进度：").append(todoState.getProgress()).append("\n");
        }
        if (todoState.getItems() != null && !todoState.getItems().isEmpty()) {
            sb.append("待办：\n");
            for (TodoItem item : todoState.getItems()) {
                String icon = "completed".equals(item.getStatus()) ? "✅"
                        : "in_progress".equals(item.getStatus()) ? "🔄"
                        : "⬜";
                sb.append(icon).append(" ").append(item.getText()).append("\n");
            }
        }
        return sb.toString();
    }
}
