package com.changyu496.agent.mini.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class TodoState {
    private String book;

    private Integer chapter;

    private String chapterTitle;

    private String progress;

    private List<TodoItem> items;

    private Date lastUpdated;
}
