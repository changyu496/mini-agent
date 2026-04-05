package com.changyu496.agent.mini.tool.manger;

import com.changyu496.agent.mini.dto.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TaskManger {

    private static final String DEFAULT_PATH = System.getProperty("user.home") + "/.reading-agent/tasks";

    private static final TaskManger instance = new TaskManger();

    private int nextId = 0;

    public static TaskManger getInstance() {
        return instance;
    }

    private TaskManger() {
        setNextId();
    }

    private void setNextId() {
        List<Task> allTask = getAllTask();
        if (allTask.size() == 0) {
            nextId = 1;
            return;
        }
        AtomicInteger id = new AtomicInteger();
        allTask.forEach(task -> {
            if (task.getId() > id.get()) {
                id.set(task.getId());
            }
        });
        nextId = id.addAndGet(1);
    }

    public void create(String subject, String description) {
        Task task = new Task();
        task.setId(nextId++);
        task.setSubject(subject);
        task.setBlockedBy(new ArrayList<>());
        if (Objects.nonNull(description)) {
            task.setDescription(description);
        } else {
            task.setDescription("");
        }
        save(task);
    }

    public void update(int taskId, String status, List<Integer> addBlockedBy, List<Integer> removeBlockedBy) {
        Task task = read(taskId);
        if (Objects.nonNull(task)) {
            task.setStatus(status);
            if ("completed".equals(status)) {
                clearDependency(taskId);
            }
            if (Objects.nonNull(task.getBlockedBy()) && !addBlockedBy.isEmpty()) {
                task.getBlockedBy().addAll(addBlockedBy);
            }
            if (Objects.nonNull(task.getBlockedBy()) && !removeBlockedBy.isEmpty()) {
                task.getBlockedBy().removeAll(removeBlockedBy);
            }
            save(task);
        }
    }

    public List<Task> list() {
        return getAllTask();
    }

    public Task getDetail(int taskId) {
        return read(taskId);
    }

    private List<Task> getAllTask() {
        Path taskPath = Path.of(DEFAULT_PATH);
        List<Task> tasks = new ArrayList<>();
        try {
            try (var stream = Files.list(taskPath)) {
                stream.forEach(filePath -> {
                    Task task = read(filePath);
                    if (Objects.nonNull(task)) {
                        tasks.add(task);
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("获取全部任务遇到异常" + e.getMessage());
        }
        return tasks;
    }

    private void clearDependency(int taskId) {
        List<Task> allTask = getAllTask();
        for (Task task : allTask) {
            if (Objects.nonNull(task.getBlockedBy()) && task.getBlockedBy().contains(taskId)) {
                task.getBlockedBy().remove((Integer) taskId);
                save(task);
            }
        }
    }

    private void addBlockedBy(int taskId, int blockedTaskId) {
        Task task = getDetail(taskId);
        if (Objects.nonNull(task)) {
            List<Integer> blockedBy = task.getBlockedBy();
            blockedBy.add(blockedTaskId);
            task.setBlockedBy(blockedBy);
            save(task);
        }
    }

    private void removeBlockedBy(int taskId, int blockedTaskId) {
        Task task = getDetail(taskId);
        if (Objects.nonNull(task)) {
            List<Integer> blockedBy = task.getBlockedBy();
            task.setBlockedBy(blockedBy.stream().filter(s -> !s.equals(blockedTaskId)).collect(Collectors.toList()));
            save(task);
        }
    }

    private void save(Task task) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Path dirPath = Path.of(DEFAULT_PATH);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            String taskJson = objectMapper.writeValueAsString(task);
            String fileName = DEFAULT_PATH + "/task_" + task.getId();
            Path filePath = Path.of(fileName);
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
            Files.writeString(filePath, taskJson);
        } catch (JsonProcessingException e) {
            System.out.println("写入任务文件遇到异常" + e.getMessage());
        } catch (IOException e) {
            System.out.println("创建任务文件遇到异常" + e.getMessage());
        }
    }

    private Task read(int taskId) {
        String fileName = DEFAULT_PATH + "/task_" + taskId;
        Path filePath = Path.of(fileName);
        return read(filePath);
    }

    private Task read(Path filePath) {
        if (!Files.exists(filePath)) {
            System.out.println("任务文件" + filePath.getFileName().toFile().getName() + "不存在");
            return null;
        }
        try {
            String taskContent = Files.readString(filePath);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(taskContent, Task.class);
        } catch (IOException e) {
            System.out.println("读取任务文件遇到异常" + e.getMessage());
            return null;
        }
    }

}
