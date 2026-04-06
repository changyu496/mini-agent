package com.changyu496.agent.mini.tool.handler;

import com.changyu496.agent.mini.Main;
import com.changyu496.agent.mini.agent.Message;

import java.util.List;

public class CompactHandler implements ToolHandler {
    @Override
    public String execute(String argJson,String functionName) {
        List<Message> result = Main.autoCompact(Main.historyMessages);
        Main.historyMessages = result;
        return "上下文已压缩,摘要:" + result.get(0).getContent();
    }
}
