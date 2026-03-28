package com.changyu496.agent.mini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {
    private String role;
    private String content;

    private String name;

    @JsonProperty("audio_content")
    private String audioContent;

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

    @Override
    public String toString() {
        return "Message{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                ", name='" + name + '\'' +
                ", audioContent='" + audioContent + '\'' +
                '}';
    }
}
