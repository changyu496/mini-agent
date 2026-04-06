package com.changyu496.agent.mini.tool.manger;

import com.changyu496.agent.mini.dto.JobInfo;
import com.changyu496.agent.mini.dto.JobNotification;

import java.util.*;
import java.util.concurrent.*;

public class BackgroundManger {

    private ConcurrentHashMap<String, JobInfo> jobInfoMap;

    private Queue<JobNotification> jobNotificationQueue;

    private ExecutorService executor;

    private static final BackgroundManger instance = new BackgroundManger();

    public static BackgroundManger getInstance() {
        return instance;
    }

    private BackgroundManger() {
        jobInfoMap = new ConcurrentHashMap<>();
        jobNotificationQueue = new ConcurrentLinkedQueue<>();
        executor = Executors.newCachedThreadPool();
    }

    public String submit(String type, String description, Callable<String> job) {
        String jobId = UUID.randomUUID().toString();
        JobInfo jobInfo = new JobInfo();
        jobInfo.setJobId(jobId);
        jobInfo.setStatus("running");
        jobInfo.setResult("");
        jobInfo.setType(type);
        jobInfo.setDescription(description);
        jobInfo.setCreateAt(new Date());

        jobInfoMap.put(jobId, jobInfo);
        executor.submit(() -> {
            String result;
            String status = "completed";
            try {
                result = job.call();
            } catch (Exception e) {
                result = "执行失败：" + e.getMessage();
                status = "error";
            }
            // 更新状态
            jobInfo.setResult(result);
            jobInfo.setStatus(status);
            // 发布消息
            JobNotification jobNotification = new JobNotification();
            jobNotification.setJobId(jobId);
            jobNotification.setStatus(status);
            jobNotification.setResult(result);
            jobNotification.setType(type);
            jobNotificationQueue.add(jobNotification);
        });
        return "后台任务已经启动，jobId：" + jobId;
    }

    public List<JobNotification> drainNotifications() {
        List<JobNotification> result = new ArrayList<>();
        JobNotification n;
        while ((n = jobNotificationQueue.poll()) != null) {
            result.add(n);
        }
        return result;
    }

    public String check(String jobId) {
        JobInfo jobInfo = jobInfoMap.get(jobId);
        return jobInfo.render();
    }

    public String listAll() {
        StringBuilder stringBuilder = new StringBuilder("当前任务列表情况如下：\n");
        jobInfoMap.values().forEach(jobInfo -> stringBuilder.append(jobInfo.render()));
        return stringBuilder.toString();
    }
}
