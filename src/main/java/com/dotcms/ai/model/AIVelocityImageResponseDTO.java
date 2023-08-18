package com.dotcms.ai.model;

public class AIVelocityImageResponseDTO {

    private String model;
    private String httpStatus;
    private String prompt;
    private String response;
    private String fileId;

    public AIVelocityImageResponseDTO(String model, String httpStatus, String prompt, String response, String fileId) {
        this.model = model;
        this.httpStatus = httpStatus;
        this.prompt = prompt;
        this.response = response;
        this.fileId = fileId;
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

    public String getFileId() {
        return fileId;
    }

    public String getModel() {
        return model;
    }
}
