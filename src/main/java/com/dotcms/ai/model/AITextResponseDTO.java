package com.dotcms.ai.model;

public class AITextResponseDTO {

    private String model;
    private String prompt;
    private String httpStatus;
    private String response;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = "OpenAI:" + model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(String httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }


}
