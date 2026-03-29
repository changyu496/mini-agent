package com.changyu496.agent.mini.tool;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;

public class ReadFileHandler implements ToolHandler {
    @Override
    public String execute(String argJson) {
        ObjectMapper objectMapper = new ObjectMapper();
        String path = "";
        try {
            Map map = objectMapper.readValue(argJson, Map.class);
            path = String.valueOf(map.get("path"));
            Path filePath = Path.of(path);
            return Files.readString(filePath);
        } catch (NoSuchFileException e) {
            return "文件不存在: " + path + "\n你可以尝试其他路径。";
        } catch (IOException e) {
            return "读取文件失败: " + e.getMessage();
        }
    }
}
