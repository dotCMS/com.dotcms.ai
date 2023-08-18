package com.dotcms.ai.model;

public class AIImageRequestDTO {

    private String prompt;

    public AIImageRequestDTO(String prompt) {
        this.prompt = prompt;
    }

    public AIImageRequestDTO() {
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

}
