package com.dotcms.ai.model;

public class AIImageRequestDTO {

    private String prompt;
    private int n=1;
    private String size;

    public AIImageRequestDTO(String prompt) {
        this.prompt = prompt;
    }

    public AIImageRequestDTO() {
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

}
