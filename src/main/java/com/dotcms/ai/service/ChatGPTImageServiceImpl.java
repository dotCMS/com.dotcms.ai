package com.dotcms.ai.service;

import com.dotcms.ai.Marshaller;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.interceptor.RequestLoggingInterceptor;
import com.dotcms.ai.model.AIImageResponseDTO;
import com.dotcms.ai.model.ChatGptImageRequestDTO;
import com.dotcms.ai.model.ChatGptImageResponseDTO;
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

    /**
     * Creates new DTO class to be used in request body sent to ChatGPT api and returns formatted response
     * @param prompt
     * @param config
     * @param isRawPrompt
     * @return
     */
    @Override
    public AIImageResponseDTO sendChatGPTRequest(String prompt, AppConfig config, boolean isRawPrompt) {
        AIImageResponseDTO aiImageResponseDTO = new AIImageResponseDTO();

        try {
            ChatGptImageRequestDTO chatGptImageRequestDTO = createChatGptImageRequest(prompt, isRawPrompt);
            aiImageResponseDTO.setPrompt(chatGptImageRequestDTO.getPrompt());
            Request request = createRequest(chatGptImageRequestDTO);

            Response response = httpClient.newCall(request).execute();
            processResponse(response, aiImageResponseDTO);
        } catch (IOException e) {
            aiImageResponseDTO.setPrompt(prompt);
            handleApiError(e, aiImageResponseDTO);
        }

        return aiImageResponseDTO;
    }

    private ChatGptImageRequestDTO createChatGptImageRequest(String prompt, boolean isRawPrompt) {
        return new ChatGptImageRequestDTO(prompt, CHAT_GPT_IMAGE_COUNT, CHAT_GPT_IMAGE_SIZE, CHAT_GPT_PROMPT_IMAGE, isRawPrompt, ConfigService.INSTANCE.config().getImageModel());
    }

    /**
     * Creates request using generated DTO
     * @param chatGptImageRequestDTO
     * @return
     * @throws IOException
     */
    private Request createRequest(ChatGptImageRequestDTO chatGptImageRequestDTO) throws IOException {
        return new Request.Builder()
            .url(CHAT_GPT_API_URL)
            .method("POST", RequestBody.create(JSON_MEDIA_TYPE, Marshaller.marshal(chatGptImageRequestDTO)))
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer " + CHAT_GPT_API_KEY)
            .build();
    }

    /**
     * Handles response from ChatGPT API. If response is OK it unmarshalls response body to DTO class.
     * If not just uses whichever response is returned from API
     * @param response
     * @param aiImageResponseDTO
     * @throws IOException
     */
    private void processResponse(Response response, AIImageResponseDTO aiImageResponseDTO) throws IOException {
        aiImageResponseDTO.setHttpStatus(String.valueOf(response.code()));

        if (HttpResponseStatus.OK.code() == response.code()) {
            ChatGptImageResponseDTO chatGptImageResponseDTO = Marshaller.unmarshal(response.body().string(), ChatGptImageResponseDTO.class);
            aiImageResponseDTO.setResponse(chatGptImageResponseDTO.getData().get(0).getUrl().toString());
        } else {
            aiImageResponseDTO.setResponse(response.body().string());
        }
    }

    /**
     * If there was an error inside plugin it is logged here and API response if modified accordingly
     * @param e
     * @param aiImageResponseDTO
     */
    private void handleApiError(IOException e, AIImageResponseDTO aiImageResponseDTO) {
        String errorMessage = "Error calling ChatGPT API: " + e.getMessage();
        Logger.error(this.getClass(), errorMessage);

        aiImageResponseDTO.setResponse(errorMessage);
        aiImageResponseDTO.setHttpStatus(String.valueOf(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()));
    }
}
