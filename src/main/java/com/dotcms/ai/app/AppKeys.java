package com.dotcms.ai.app;

public enum AppKeys {
    API_URL("apiUrl"),
    API_IMAGE_URL("apiImageUrl"),
    API_KEY("apiKey"),
    ROLE_PROMPT("rolePrompt"),
    TEXT_PROMPT("textPrompt"),
    IMAGE_PROMPT("imagePrompt"),
    IMAGE_SIZE("imageSize"),
    MODEL("model"),
    COMPLETION_MODEL("com.dotcms.ai.completion.model"),
    COMPLETION_ROLE_PROMPT("com.dotcms.ai.completion.role.prompt"),
    COMPLETION_TEXT_PROMPT("com.dotcms.ai.completion.text.prompt"),
    COMPLETION_API_RATE_LIMIT("com.dotcms.ai.completions.api.rpm"),
    EMBEDDINGS_MAX_TOKENS("com.dotcms.ai.embeddings.max.tokens"),
    EMBEDDINGS_MODEL("com.dotcms.ai.embeddings.model"),
    EMBEDDINGS_SPLIT_AT_WORDS("com.dotcms.ai.embeddings.split.at.words"),
    EMBEDDINGS_API_RATE_LIMIT("com.dotcms.ai.embeddings.api.rpm"),
    EMBEDDINGS_MINIMUM_FILE_SIZE_TO_INDEX("com.dotcms.ai.embeddings.minimum.file.size"),
    EMBEDDINGS_SPLIT_DOCUMENTS_AT_WORDS("com.dotcms.ai.embeddings.split.at.words"),
    EMBEDDINGS_FILE_EXTENSIONS_TO_EMBED("com.dotcms.ai.embeddings.build.for.file.extensions"),
    EMBEDDINGS_THREADS("com.dotcms.ai.embeddings.threads"),
    EMBEDDINGS_THREADS_MAX("com.dotcms.ai.embeddings.threads.max"),
    EMBEDDINGS_THREADS_QUEUE("com.dotcms.ai.embeddings.threads.queue"),
    EMBEDDINGS_CACHE_TTL_SECONDS("com.dotcms.ai.embeddings.cache.size"),
    EMBEDDINGS_CACHE_SIZE("com.dotcms.ai.embeddings.cache.ttl.seconds");

    public static final String APP_KEY = "dotAI";
    public static final String APP_YAML_NAME = APP_KEY + ".yml";
    final public String key;

    AppKeys(String key) {
        this.key = key;
    }
}
