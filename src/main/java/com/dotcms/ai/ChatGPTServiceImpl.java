package com.dotcms.ai;

import com.dotcms.ai.interceptor.RequestLoggingInterceptor;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.model.AITextResponseDTO;
import com.dotcms.ai.model.ChatGptRequestDTO;
import com.dotcms.ai.model.ChatGptResponseDTO;
import com.dotmarketing.util.Logger;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatGPTServiceImpl implements ChatGPTService {

    private static String CHAT_GPT_API_URL;

    private static String CHAT_GPT_API_KEY;

    private static final String CHAT_GPT_ROLE = "user";

    private static String CHAT_GPT_MODEL;

    private static String CHAT_GPT_PROMPT_ROLE;

    private static String CHAT_GPT_PROMPT_TEXT;

    private static String CHAT_GPT_PROMPT_IMAGE;

    private static final long READ_TIMEOUT = 30;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private OkHttpClient httpClient;

    public ChatGPTServiceImpl(AppConfig appConfig) {
        this.httpClient = new OkHttpClient().newBuilder()
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(new RequestLoggingInterceptor())
            .build();
        CHAT_GPT_API_URL = appConfig.getApiUrl();
        CHAT_GPT_API_KEY = appConfig.getApiKey();
        CHAT_GPT_MODEL = appConfig.getModel();
        CHAT_GPT_PROMPT_ROLE = appConfig.getRolePrompt();
        CHAT_GPT_PROMPT_TEXT = appConfig.getTextPrompt();
        CHAT_GPT_PROMPT_IMAGE = appConfig.getImagePrompt();
    }

    @Override
    public AITextResponseDTO sendChatGPTRequest(String prompt, Optional<AppConfig> config) {

        ChatGptRequestDTO chatGptRequestDTO = new ChatGptRequestDTO(CHAT_GPT_MODEL, CHAT_GPT_ROLE, CHAT_GPT_PROMPT_ROLE, CHAT_GPT_PROMPT_TEXT, CHAT_GPT_PROMPT_IMAGE, prompt);

        AITextResponseDTO aiTextResponseDTO = new AITextResponseDTO();
        aiTextResponseDTO.setModel(chatGptRequestDTO.getModel());
        aiTextResponseDTO.setPrompt(chatGptRequestDTO.getMessages().get(0).getContent());

        try {
            Request request = new Request.Builder()
                .url(CHAT_GPT_API_URL)
                .method("POST", RequestBody.create(JSON_MEDIA_TYPE, Marshaller.marshal(chatGptRequestDTO)))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + CHAT_GPT_API_KEY)
                .build();

            Response response = httpClient.newCall(request).execute();

            if (HttpResponseStatus.OK.code() == response.code()) {
                ChatGptResponseDTO chatGptResponseDTO = Marshaller.unmarshal(response.body().string(), ChatGptResponseDTO.class);
                aiTextResponseDTO.setResponse(chatGptResponseDTO.getChoices().get(0).getMessage().getContent());
            } else {
                aiTextResponseDTO.setResponse(String.format("Error calling ChatGPT API: [code=%s] [message=%s] [body=%s]", response.code(), response.message(), response.body().string()));
             }
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error calling ChatGPT API: " + e.getMessage());
            aiTextResponseDTO.setResponse("Error calling ChatGPT API: " + e.getMessage());
        }

        return aiTextResponseDTO;
    }
}