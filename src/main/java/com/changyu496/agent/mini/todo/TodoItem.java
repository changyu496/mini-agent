package com.changyu496.agent.mini.todo;

import lombok.Data;

@Data
public class TodoItem {
    String id;
    String text;
    /**
     * pending/in_progress/completed
     */
    String status;
}
