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
        return "任务详情如下:" + "jobId：" + jobId +
                "状态：" + status +
                "结果：" + result +
                "类型：" + type +
                "描述：" + description +
                "创建时间：" + createAt.toString();
    }

    @Override
    public String toString() {
        return "JobInfo{" +
                "jobId='" + jobId + '\'' +
                ", status='" + status + '\'' +
                ", result='" + result + '\'' +
                ", type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", createAt=" + createAt +
                '}';
    }
}
