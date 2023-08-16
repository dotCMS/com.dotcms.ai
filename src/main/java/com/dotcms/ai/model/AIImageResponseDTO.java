package com.dotcms.ai.model;

public class AIImageResponseDTO {

    private String prompt;
    private String imageUrl;
    private int chatGPTResponseStatus;
    private String errorMessage;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getChatGPTResponseStatus() {
        return chatGPTResponseStatus;
    }

    public void setChatGPTResponseStatus(int chatGPTResponseStatus) {
        this.chatGPTResponseStatus = chatGPTResponseStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
