package com.changyu496.agent.mini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OpenAIResponse {
    private String id;

    private List<Choice> choices;

    private Long created;

    private String model;

    private String object;

    private Usage usage;

    @JsonProperty("input_sensitive")
    private Boolean inputSensitive;
    @JsonProperty("output_sensitive")
    private Boolean outputSensitive;

    @JsonProperty("input_sensitive_type")
    private Integer inputSensitiveType;
    @JsonProperty("output_sensitive_type")
    private Integer outputSensitiveType;

    @JsonProperty("output_sensitive_int")
    private Integer outputSensitiveInt;

    @JsonProperty("base_resp")
    private BaseResp baseResp;

    private static class BaseResp{
        @JsonProperty("status_code")
        private Integer statusCode;

        @JsonProperty("status_msg")
        private String statusMsg;

        public Integer getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
        }

        public String getStatusMsg() {
            return statusMsg;
        }

        public void setStatusMsg(String statusMsg) {
            this.statusMsg = statusMsg;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public Boolean getInputSensitive() {
        return inputSensitive;
    }

    public void setInputSensitive(Boolean inputSensitive) {
        this.inputSensitive = inputSensitive;
    }

    public Boolean getOutputSensitive() {
        return outputSensitive;
    }

    public void setOutputSensitive(Boolean outputSensitive) {
        this.outputSensitive = outputSensitive;
    }

    public Integer getInputSensitiveType() {
        return inputSensitiveType;
    }

    public void setInputSensitiveType(Integer inputSensitiveType) {
        this.inputSensitiveType = inputSensitiveType;
    }

    public Integer getOutputSensitiveType() {
        return outputSensitiveType;
    }

    public void setOutputSensitiveType(Integer outputSensitiveType) {
        this.outputSensitiveType = outputSensitiveType;
    }

    public BaseResp getBaseResp() {
        return baseResp;
    }

    public void setBaseResp(BaseResp baseResp) {
        this.baseResp = baseResp;
    }
}
