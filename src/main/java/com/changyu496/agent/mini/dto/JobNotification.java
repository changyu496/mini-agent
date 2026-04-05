package com.changyu496.agent.mini.dto;

import lombok.Data;

@Data
public class JobNotification {
    private String jobId;
    private String status;
    private String result;
    private String type;
}
