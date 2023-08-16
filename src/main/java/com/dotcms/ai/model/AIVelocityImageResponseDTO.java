package com.dotcms.ai.model;

public class AIVelocityImageResponseDTO {

    private int httpStatus;
    private String request;
    private String response;

    public AIVelocityImageResponseDTO(int httpStatus, String request, String response) {
        this.httpStatus = httpStatus;
        this.request = request;
        this.response = response;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getRequest() {
        return request;
    }

    public String getResponse() {
        return response;
    }
}
