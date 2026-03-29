package com.changyu496.agent.mini.tool;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SearchNotesHandler implements ToolHandler {

    private static final String DEFAULT_PATH = "/Users/changyu/.reading-agent/workspace";

    @Override
    public String execute(String argJson) {
        StringBuilder stringBuilder = new StringBuilder();
        Map arg;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            arg = objectMapper.readValue(argJson, Map.class);
            String keyword = String.valueOf(arg.get("keyword"));
            String dir = String.valueOf(arg.get("dir"));
            if (dir == null || dir.isEmpty() || "null".equals(dir)) {
                dir = DEFAULT_PATH;
            }
            // 遍历指定文件夹
            Path dirPath = Path.of(dir);
            List<Path> filePath = Files.list(dirPath).toList();
            // 先只考虑一层
            int fileCount = 0;
            int matchCount = 0;
            if (filePath.size() > 0) {
                for (Path path : filePath) {
                    List<String> singleFileFindResult = new ArrayList<>();
                    try {
                        List<String> line = Files.readAllLines(path);
                        for (int j = 0; j < line.size(); j++) {
                            if (line.get(j).contains(keyword)) {
                                singleFileFindResult.add("-第" + (j + 1) + "行" + line.get(j));
                            }
                        }
                        if (singleFileFindResult.size() > 0) {
                            fileCount++;
                            stringBuilder.append("文件：")
                                    .append(path.toAbsolutePath())
                                    .append("\r\n")
                                    .append(convertSingleFindResult(singleFileFindResult));
                        }
                        matchCount += singleFileFindResult.size();
                    } catch (IOException e) {
                        return "查找笔记遇到异常" + e.getMessage();
                    }
                }
                stringBuilder.append("\r\n").append("共匹配 ")
                        .append(fileCount)
                        .append(" 个文件，")
                        .append(matchCount)
                        .append(" 处记录。");
            }
        } catch (IOException e) {
            return "查找笔记遇到异常" + e.getMessage();
        }
        return stringBuilder.toString();

    }

    private String convertSingleFindResult(List<String> singleFileFindResult) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : singleFileFindResult) {
            stringBuilder.append(s).append("\r\n");
        }
        return stringBuilder.toString();
    }
}
