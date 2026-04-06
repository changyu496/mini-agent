package com.changyu496.agent.mini.background;

import lombok.Data;

@Data
public class JobNotification {
    private String jobId;
    private String status;
    private String result;
    private String type;

    public String render() {
        String stringBuilder = "任务状况：" + "任务ID：" + jobId + "\n" +
                "任务状态" + status + "\n" +
                "任务结果" + result + "\n" +
                "任务类型" + type + "\n";
        return stringBuilder;
    }
}
