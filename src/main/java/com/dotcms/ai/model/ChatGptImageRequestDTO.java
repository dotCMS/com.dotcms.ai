package com.dotcms.ai.model;

public class ChatGptImageRequestDTO {

    private String prompt;
    private int n;
    private String size;

    public ChatGptImageRequestDTO() {
    }

    public ChatGptImageRequestDTO(String promptInput, int n, String size, String promptImage, boolean rawPrompt) {
        String input = rawPrompt ? promptInput : (promptImage + " " + promptInput);
        if (input.length() > 999)
            input = input.substring(0, 999);
        this.prompt = input;
        this.n = n;
        this.size = size;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

}
