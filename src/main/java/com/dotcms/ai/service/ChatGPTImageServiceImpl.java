package com.dotcms.ai.service;

import com.dotcms.ai.Marshaller;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.interceptor.RequestLoggingInterceptor;
import com.dotcms.ai.model.AIImageResponseDTO;
import com.dotcms.ai.model.ChatGptImageRequestDTO;
import com.dotcms.ai.model.ChatGptImageResponseDTO;
import com.dotmarketing.util.Logger;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatGPTImageServiceImpl implements ChatGPTImageService{

    private static String CHAT_GPT_API_URL;

    private static String CHAT_GPT_API_KEY;

    private static String CHAT_GPT_PROMPT_IMAGE;

    private static String CHAT_GPT_IMAGE_SIZE;

    private static int CHAT_GPT_IMAGE_COUNT = 1;

    private static final long READ_TIMEOUT = 30;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private OkHttpClient httpClient;

    public ChatGPTImageServiceImpl(AppConfig appConfig) {
        this.httpClient = new OkHttpClient().newBuilder()
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(new RequestLoggingInterceptor())
            .build();
        CHAT_GPT_API_URL = appConfig.getApiImageUrl();
        CHAT_GPT_API_KEY = appConfig.getApiKey();
        CHAT_GPT_PROMPT_IMAGE = appConfig.getImagePrompt();
        CHAT_GPT_IMAGE_SIZE = appConfig.getImageSize();
    }

    @Override
    public AIImageResponseDTO sendChatGPTRequest(String prompt, Optional<AppConfig> config, boolean isRawPrompt) {

        ChatGptImageRequestDTO chatGptImageRequestDTO = new ChatGptImageRequestDTO(prompt, CHAT_GPT_IMAGE_COUNT, CHAT_GPT_IMAGE_SIZE, CHAT_GPT_PROMPT_IMAGE, isRawPrompt);

        AIImageResponseDTO aiImageResponseDTO = new AIImageResponseDTO();
        aiImageResponseDTO.setPrompt(chatGptImageRequestDTO.getPrompt());

        try {
            Request request = new Request.Builder()
                .url(CHAT_GPT_API_URL)
                .method("POST", RequestBody.create(JSON_MEDIA_TYPE, Marshaller.marshal(chatGptImageRequestDTO)))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + CHAT_GPT_API_KEY)
                .build();

            Response response = httpClient.newCall(request).execute();
            aiImageResponseDTO.setChatGPTResponseStatus(response.code());

            if (HttpResponseStatus.OK.code() == response.code()) {
                ChatGptImageResponseDTO chatGptImageResponseDTO = Marshaller.unmarshal(response.body().string(), ChatGptImageResponseDTO.class);
                aiImageResponseDTO.setImageUrl(chatGptImageResponseDTO.getData().get(0).getUrl().toString());
            } else {
                aiImageResponseDTO.setErrorMessage(response.body().string());
            }
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error calling ChatGPT API: " + e.getMessage());
            aiImageResponseDTO.setErrorMessage("Error calling ChatGPT API: " + e.getMessage());
            aiImageResponseDTO.setChatGPTResponseStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        }

        return aiImageResponseDTO;
    }
}
