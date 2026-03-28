package com.changyu496.agent.mini.dto;

public class ReasoningDetail {

    private String type;

    private String id;

    private String format;

    private Integer index;

    private String text;

    @Override
    public String toString() {
        return "ReasoningDetail{" +
                "type='" + type + '\'' +
                ", id='" + id + '\'' +
                ", format='" + format + '\'' +
                ", index=" + index +
                ", text='" + text + '\'' +
                '}';
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
