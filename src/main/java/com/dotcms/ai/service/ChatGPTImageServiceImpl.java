package com.dotcms.ai.service;

import com.dotcms.ai.Marshaller;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.model.AIImageResponseDTO;
import com.dotcms.ai.model.ChatGptImageRequestDTO;
import com.dotcms.ai.model.ChatGptImageResponseDTO;
import com.dotcms.ai.util.Logger;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotmarketing.util.json.JSONObject;
import java.io.IOException;

public class ChatGPTImageServiceImpl implements ChatGPTImageService{



    private static int CHAT_GPT_IMAGE_COUNT = 1;

    private static final long READ_TIMEOUT = 30;

    private final AppConfig config;

    public ChatGPTImageServiceImpl(AppConfig appConfig) {


        config=appConfig;
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
        aiImageResponseDTO.setPrompt(prompt);
        aiImageResponseDTO.setModel(config.getImageModel());
        try {
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("model", config.getImageModel());
            jsonRequest.put("prompt", prompt + " " + (isRawPrompt ? "" : config.getImagePrompt()));
            jsonRequest.put("size",config.getImageSize());


            String responseString = OpenAIRequest.doRequest(config.getApiImageUrl(),"POST",config.getApiKey(),jsonRequest );
            JSONObject jsonResponse = (JSONObject) new JSONObject(responseString).getJSONArray("data").get(0);
            aiImageResponseDTO.setPrompt(jsonResponse.optString("revised_prompt", prompt));
            aiImageResponseDTO.setUrl(jsonResponse.optString("url"));
            aiImageResponseDTO.setHttpStatus("200");
            aiImageResponseDTO.setResponse(responseString);
            return aiImageResponseDTO;
        } catch (Exception e) {
            Logger.warn(this.getClass(), e.getMessage(), e);
            aiImageResponseDTO.setResponse(e.getMessage());
            aiImageResponseDTO.setHttpStatus("500");
            return aiImageResponseDTO;
        }

    }

    private ChatGptImageRequestDTO createChatGptImageRequest(String prompt, boolean isRawPrompt) {
        return new ChatGptImageRequestDTO(prompt, CHAT_GPT_IMAGE_COUNT, config.getImageSize(), config.getImagePrompt(),
                isRawPrompt, config.getImageModel());
    }


    /**
     * Handles response from ChatGPT API. If response is OK it unmarshalls response body to DTO class.
     * If not just uses whichever response is returned from API
     * @param response
     * @throws IOException
     */
    private ChatGptImageResponseDTO processResponse(String response) throws IOException {

           return Marshaller.unmarshal(response, ChatGptImageResponseDTO.class);

    }


}
