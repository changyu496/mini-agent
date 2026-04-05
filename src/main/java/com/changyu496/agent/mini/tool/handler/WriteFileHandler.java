package com.changyu496.agent.mini.tool.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class WriteFileHandler implements ToolHandler {
    @Override
    public String execute(String argJson, String functionName) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map argMap = objectMapper.readValue(argJson, Map.class);
            String pathStr = String.valueOf(argMap.get("path"));
            String content = String.valueOf(argMap.get("content"));
            Path path = Path.of(pathStr);
            Files.writeString(path, content);
            return "文件写入成功: " + pathStr;
        } catch (JsonProcessingException e) {
            return "参数有问题，请检查";
        } catch (IOException e) {
            return "文件写入失败，异常为" + e.getMessage();
        }
    }
}
