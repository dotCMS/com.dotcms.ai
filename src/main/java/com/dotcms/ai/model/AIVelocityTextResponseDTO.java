package com.dotcms.ai.model;

public class AIVelocityTextResponseDTO {

    private int httpStatus;
    private String request;
    private String response;

    public AIVelocityTextResponseDTO(int httpStatus, String response, String request) {
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
