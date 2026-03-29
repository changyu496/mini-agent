package com.changyu496.agent.mini.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class WriteFileHandler implements ToolHandler {
    @Override
    public String execute(String argJson) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> argMap = objectMapper.readValue(argJson, Map.class);
            String pathStr = String.valueOf(argMap.get("path"));
            String content = String.valueOf(argMap.get("content"));
            Path path = Path.of(pathStr);
            Path writePath = Files.writeString(path, content);
            return "文件已经写入成功，位置为:" + writePath.getFileName().getFileName();
        } catch (JsonProcessingException e) {
            return "参数有问题，请检查";
        } catch (IOException e) {
            return "文件写入失败，异常为" + e.getMessage();
        }
    }
}
