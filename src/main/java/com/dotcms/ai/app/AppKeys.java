package com.dotcms.ai.app;

public enum AppKeys {
    API_URL("apiUrl", "https://api.openai.com/v1/chat/completions"),
    API_IMAGE_URL("apiImageUrl", "https://api.openai.com/v1/images/generations"),
    API_KEY("apiKey", null),
    ROLE_PROMPT("rolePrompt", "You are dotCMSbot, and AI assistant to help content creators generate and rewrite content in their content management system."),
    TEXT_PROMPT("textPrompt", "Use Descriptive writting style."),
    IMAGE_PROMPT("imagePrompt", "Use 16:9 aspect ratio."),
    IMAGE_SIZE("imageSize", "1024x1024"),
    MODEL("model", "gpt-3.5-turbo-16k"),
    COMPLETION_MODEL("com.dotcms.ai.completion.model", "gpt-3.5-turbo-16k"),
    COMPLETION_ROLE_PROMPT("com.dotcms.ai.completion.role.prompt", "Use Descriptive writting style."),
    COMPLETION_TEXT_PROMPT("com.dotcms.ai.completion.text.prompt", "Answer this question\\n\\\"${query}?\\\"\\n\\nby summarizing the following text:\\n\\n${supportingContent}"),
    EMBEDDINGS_MODEL("com.dotcms.ai.embeddings.model", "text-embedding-ada-002"),
    EMBEDDINGS_SPLIT_AT_WORDS("com.dotcms.ai.embeddings.split.at.words", "65"),
    EMBEDDINGS_MINIMUM_FILE_SIZE_TO_INDEX("com.dotcms.ai.embeddings.minimum.file.size", "1024"),
    EMBEDDINGS_FILE_EXTENSIONS_TO_EMBED("com.dotcms.ai.embeddings.build.for.file.extensions", "pdf,doc,docx,txt,html"),
    EMBEDDINGS_THREADS("com.dotcms.ai.embeddings.threads", "3"),
    EMBEDDINGS_THREADS_MAX("com.dotcms.ai.embeddings.threads.max", "10"),
    EMBEDDINGS_THREADS_QUEUE("com.dotcms.ai.embeddings.threads.queue", "10000"),
    EMBEDDINGS_CACHE_TTL_SECONDS("com.dotcms.ai.embeddings.cache.size", "1000"),
    EMBEDDINGS_CACHE_SIZE("com.dotcms.ai.embeddings.cache.ttl.seconds", "600");

    public static final String APP_KEY = "dotAI";
    public static final String APP_YAML_NAME = APP_KEY + ".yml";
    final public String key;
    final public String defaultValue;

    AppKeys(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }
}
