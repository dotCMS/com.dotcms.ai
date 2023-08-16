package com.dotcms.ai.app;

import java.io.Serializable;

public class AppConfig implements Serializable {

    private final String apiUrl;

    private final String apiImageUrl;

    private final String apiKey;

    private final String rolePrompt;

    private final String textPrompt;

    private final String imagePrompt;

    private final String imageSize;

    private final String model;

    public AppConfig(String apiUrl, String apiImageUrl, String apiKey, String rolePrompt, String textPrompt, String imagePrompt, String imageSize, String model) {
        this.apiUrl = apiUrl;
        this.apiImageUrl = apiImageUrl;
        this.apiKey = apiKey;
        this.rolePrompt = rolePrompt;
        this.textPrompt = textPrompt;
        this.imagePrompt = imagePrompt;
        this.imageSize = imageSize;
        this.model = model;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public java.lang.String getApiImageUrl() {
        return apiImageUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getRolePrompt() {
        return rolePrompt;
    }

    public String getTextPrompt() {
        return textPrompt;
    }

    public String getImagePrompt() {
        return imagePrompt;
    }

    public String getImageSize() {
        return imageSize;
    }

    public String getModel() {
        return model;
    }


}
