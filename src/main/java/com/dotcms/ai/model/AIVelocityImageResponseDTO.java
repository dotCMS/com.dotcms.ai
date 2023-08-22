package com.dotcms.ai.model;

public class AIVelocityImageResponseDTO {

    private String model;
    private String httpStatus;
    private String prompt;
    private String response;

    public AIVelocityImageResponseDTO(String model, String httpStatus, String prompt, String response) {
        this.model = model;
        this.httpStatus = httpStatus;
        this.prompt = prompt;
        this.response = response;
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

    public String getModel() {
        return model;
    }
}
