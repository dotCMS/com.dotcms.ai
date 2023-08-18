package com.dotcms.ai.model;

public class AIVelocityTextResponseDTO {

    private String model;
    private String httpStatus;
    private String prompt;
    private String response;

    public AIVelocityTextResponseDTO(String model, String httpStatus, String request, String response) {
        this.model = model;
        this.httpStatus = httpStatus;
        this.prompt = request;
        this.response = response;
    }

    public String getModel() {
        return model;
    }

    public String getHttpStatus() {
        return httpStatus;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getResponse() {
        return response;
    }


}
