package com.changyu496.agent.mini.dto;

import lombok.Data;

import java.time.LocalTime;
import java.util.Date;

@Data
public class JobInfo {
    private String jobId;

    /**
     * running/completed/error/timeout
     */
    private String status;

    /**
     * 任务执行结果
     */
    private String result;
    /**
     * sub_agent/bash
     */
    private String type;
    /**
     * 任务描述
     */
    private String description;
    private Date createAt;

    public String render() {
        return "任务详情如下:" + "jobId：" + jobId + "\n" +
                "状态：" + status + "\n" +
                "结果：" + result + "\n" +
                "类型：" + type + "\n" +
                "描述：" + description + "\n" +
                "创建时间：" + createAt.toString();
    }
}
