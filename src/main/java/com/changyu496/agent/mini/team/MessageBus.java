package com.changyu496.agent.mini.team;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static java.nio.file.StandardOpenOption.CREATE;

public class MessageBus {

    private final Path inboxDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessageBus(Path inboxDir) {
        this.inboxDir = inboxDir;
        try {
            Files.createDirectories(inboxDir);
        } catch (IOException e) {
            System.out.println("创建目录遇到异常" + e.getMessage());
        }
    }

    public String send(String sender, String to, String content, String messageType, Map<String, Object> extra) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", messageType);
        message.put("sender", sender);
        message.put("content", content);
        message.put("timestamp", System.currentTimeMillis());

        if (Objects.nonNull(extra)) {
            message.putAll(extra);
        }

        Path inboxPath = inboxDir.resolve(to + ".jsonl");

        if (!Files.exists(inboxPath)) {
            try {
                Files.createFile(inboxPath);
            } catch (IOException e) {
                System.out.println("创建消息遇到异常" + e.getMessage());
                return "发生消息失败，遇到异常" + e.getMessage();
            }
        }
        try {
            Files.writeString(inboxPath, objectMapper.writeValueAsString(message) + "\n", StandardOpenOption.APPEND, CREATE);
        } catch (IOException e) {
            System.out.println("写入消息文件遇到异常" + e.getMessage());
        }
        return "消息发送成功";
    }

    public String send(String sender, String to, String content, String messageType) {
        return send(sender, to, content, messageType, null);
    }

    public List<Map<String, Object>> readInbox(String name) {
        Path inboxPath = inboxDir.resolve(name + ".jsonl");
        if (!Files.exists(inboxPath)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        try {
            String content = Files.readString(inboxPath);
            if (Objects.isNull(content) || content.isBlank()) {
                return messages;
            }
            for (String line : content.split("\n")) {
                if (!line.isBlank()) {
                    messages.add(objectMapper.readValue(line, Map.class));
                }
            }
        } catch (IOException e) {
            System.out.println("读取消息记录遇到异常");
        }
        try {
            Files.writeString(inboxPath, "");
        } catch (IOException e) {
            System.out.println("清空消息记录遇到异常");
        }
        return messages;
    }

    public String broadcast(String sender, String content, List<String> teammates) {
        int count = 0;
        for (String name : teammates) {
            if (!name.equals(sender)) {
                send(sender, name, content, "broadcast");
                count++;
            }
        }
        return "已经广播给" + count + "个队友";
    }
}
