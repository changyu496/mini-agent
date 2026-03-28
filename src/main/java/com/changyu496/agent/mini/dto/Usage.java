package com.changyu496.agent.mini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Usage {

    @JsonProperty("total_tokens")
    private Integer totalTokens;

    @JsonProperty("total_characters")
    private Integer totalCharacters;

    @JsonProperty("prompt_tokens")
    private Integer promptTokens;

    @JsonProperty("completion_tokens")
    private Integer completionTokens;

    @JsonProperty("completion_tokens_details")
    private CompletionTokensDetails completionTokensDetails;

    private static class CompletionTokensDetails{
        @JsonProperty("reasoning_tokens")
        private Integer reasoningTokens;

        public Integer getReasoningTokens() {
            return reasoningTokens;
        }

        public void setReasoningTokens(Integer reasoningTokens) {
            this.reasoningTokens = reasoningTokens;
        }
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Integer getTotalCharacters() {
        return totalCharacters;
    }

    public void setTotalCharacters(Integer totalCharacters) {
        this.totalCharacters = totalCharacters;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public CompletionTokensDetails getCompletionTokensDetails() {
        return completionTokensDetails;
    }

    public void setCompletionTokensDetails(CompletionTokensDetails completionTokensDetails) {
        this.completionTokensDetails = completionTokensDetails;
    }
}
