package com.changyu496.agent.mini.task;

import lombok.Data;

import java.util.List;

@Data
public class Task {
    private int id;
    private String subject;
    private String description;
    /**
     * pending
     * in_progress
     * completed
     */
    private String status;
    private List<Integer> blockedBy;
    private String owner;

    public String render() {
        return "任务ID：" + id + "\n" +
                "任务标题：" + subject + "\n" +
                "任务描述：" + description + "\n" +
                "任务状态：" + status + "\n" +
                "前置任务ID为" + (blockedBy == null ? "无" : blockedBy.toString());
    }
}
