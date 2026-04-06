package com.changyu496.agent.mini.agent;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Choice {
    @JsonProperty("finish_reason")
    private String finishReason;

    private Integer index;

    private Message message;

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Choice{" +
                "finishReason='" + finishReason + '\'' +
                ", index=" + index +
                ", message=" + message +
                '}';
    }
}
