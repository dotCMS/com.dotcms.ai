package com.dotcms.ai.app;

public enum AppKeys {
    API_URL("apiUrl"),
    API_KEY("apiKey"),
    ROLE_PROMPT("rolePrompt"),
    TEXT_PROMPT("textPrompt"),
    IMAGE_PROMPT("imagePrompt"),
    MODEL("model");

    final public String key;

    AppKeys(String key){
        this.key=key;
    }

    public final static String APP_KEY = "dotAI";

    public final static String APP_YAML_NAME = APP_KEY + ".yml";
}
