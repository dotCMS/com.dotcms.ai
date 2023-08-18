package com.dotcms.ai.service;

import com.dotcms.ai.Marshaller;
import com.dotcms.ai.interceptor.RequestLoggingInterceptor;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.model.AITextResponseDTO;
import com.dotcms.ai.model.ChatGptRequestDTO;
import com.dotcms.ai.model.ChatGptResponseDTO;
import com.dotmarketing.util.Logger;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatGPTTextServiceImpl implements ChatGPTTextService {

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

    public ChatGPTTextServiceImpl(AppConfig appConfig) {
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
    public AITextResponseDTO sendChatGPTRequest(String prompt, Optional<AppConfig> config, boolean isRawPrompt) {
        AITextResponseDTO aiTextResponseDTO = new AITextResponseDTO();

        try {
            ChatGptRequestDTO chatGptRequestDTO = createChatGptRequest(prompt, isRawPrompt);
            aiTextResponseDTO.setPrompt(chatGptRequestDTO.getMessages().get(0).getContent());
            aiTextResponseDTO.setModel(chatGptRequestDTO.getModel());

            Request request = createRequest(chatGptRequestDTO);

            Response response = httpClient.newCall(request).execute();
            processResponse(response, aiTextResponseDTO);
        } catch (IOException e) {
            aiTextResponseDTO.setPrompt(prompt);
            handleApiError(e, aiTextResponseDTO);
        }

        return aiTextResponseDTO;
    }

    private ChatGptRequestDTO createChatGptRequest(String prompt, boolean isRawPrompt) {
        return new ChatGptRequestDTO(CHAT_GPT_MODEL, CHAT_GPT_ROLE, CHAT_GPT_PROMPT_ROLE, CHAT_GPT_PROMPT_TEXT, prompt, isRawPrompt);
    }

    private Request createRequest(ChatGptRequestDTO chatGptRequestDTO) throws IOException {
        return new Request.Builder()
            .url(CHAT_GPT_API_URL)
            .method("POST", RequestBody.create(JSON_MEDIA_TYPE, Marshaller.marshal(chatGptRequestDTO)))
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer " + CHAT_GPT_API_KEY)
            .build();
    }

    private void processResponse(Response response, AITextResponseDTO aiTextResponseDTO) throws IOException {
        aiTextResponseDTO.setHttpStatus(String.valueOf(response.code()));
        if (HttpResponseStatus.OK.code() == response.code()) {
            ChatGptResponseDTO chatGptResponseDTO = Marshaller.unmarshal(response.body().string(), ChatGptResponseDTO.class);
            aiTextResponseDTO.setResponse(chatGptResponseDTO.getChoices().get(0).getMessage().getContent());
        } else {
            aiTextResponseDTO.setResponse(String.format("Error calling ChatGPT API: [code=%s] [message=%s] [body=%s]", response.code(), response.message(), response.body().string()));
        }
    }

    private void handleApiError(IOException e, AITextResponseDTO aiTextResponseDTO) {
        String errorMessage = "Error calling ChatGPT API: " + e.getMessage();
        Logger.error(this.getClass(), errorMessage);
        aiTextResponseDTO.setHttpStatus(String.valueOf(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()));
        aiTextResponseDTO.setResponse(errorMessage);
    }
}