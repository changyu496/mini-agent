package com.changyu496.agent.mini.tool;

import java.util.Map;

public class ToolDefinition {
    private String name;
    private String description;
    private Map<String,Object> parameterSchema;

    ToolHandler toolHandler;

    public ToolDefinition(String name, String description, Map<String, Object> parameterSchema, ToolHandler toolHandler) {
        this.name = name;
        this.description = description;
        this.parameterSchema = parameterSchema;
        this.toolHandler = toolHandler;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getParameterSchema() {
        return parameterSchema;
    }

    public void setParameterSchema(Map<String, Object> parameterSchema) {
        this.parameterSchema = parameterSchema;
    }

    public ToolHandler getToolHandler() {
        return toolHandler;
    }

    public void setToolHandler(ToolHandler toolHandler) {
        this.toolHandler = toolHandler;
    }
}
