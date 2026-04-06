package com.changyu496.agent.mini.agent;

public class ToolCall {
    private String id;
    private String type;
    private Function function;
    private Integer index;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Function getFunction() {
        return function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "ToolCall{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", function=" + function +
                ", index=" + index +
                '}';
    }
}
