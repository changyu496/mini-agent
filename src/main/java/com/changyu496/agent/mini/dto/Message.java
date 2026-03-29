package com.changyu496.agent.mini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Message {
    private String role;
    private String content;
    private String name;

    @JsonProperty("tool_call_id")
    private String toolCallId;

    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;

    @JsonProperty("audio_content")
    private String audioContent;

    @JsonProperty("reasoning_details")
    private List<ReasoningDetail> reasoningDetails;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAudioContent() {
        return audioContent;
    }

    public void setAudioContent(String audioContent) {
        this.audioContent = audioContent;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public List<ReasoningDetail> getReasoningDetails() {
        return reasoningDetails;
    }

    public void setReasoningDetails(List<ReasoningDetail> reasoningDetails) {
        this.reasoningDetails = reasoningDetails;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    @Override
    public String toString() {
        return "Message{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                ", name='" + name + '\'' +
                ", toolCallId='" + toolCallId + '\'' +
                ", toolCalls=" + toolCalls +
                ", audioContent='" + audioContent + '\'' +
                ", reasoningDetails=" + reasoningDetails +
                '}';
    }
}
