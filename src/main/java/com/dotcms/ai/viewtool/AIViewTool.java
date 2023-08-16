package com.dotcms.ai.viewtool;

import com.dotcms.ai.model.AIImageResponseDTO;
import com.dotcms.ai.model.AIVelocityImageResponseDTO;
import com.dotcms.ai.service.ChatGPTImageService;
import com.dotcms.ai.service.ChatGPTImageServiceImpl;
import com.dotcms.ai.service.ChatGPTTextService;
import com.dotcms.ai.service.ChatGPTTextServiceImpl;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.model.AITextResponseDTO;
import com.dotcms.ai.model.AIVelocityTextResponseDTO;
import java.io.IOException;
import java.util.Optional;
import org.apache.velocity.tools.view.tools.ViewTool;

public class AIViewTool implements ViewTool {

    @Override
    public void init(Object initData) {
    }

    /**
     * Generate a response from the AI prompt service with adding config data to original prompt (rolePrompt, textPrompt, imagePrompt)
     *
     * @return AIVelocityTextResponseDTO
     */
    public AIVelocityTextResponseDTO textGenerate(final String prompt) throws IOException {
        return generateTextResponse(prompt, false);
    }

    /**
     * Generate a raw response from the AI prompt service w/o adding data from config to prompt
     *
     * @return AIVelocityTextResponseDTO
     */
    public AIVelocityTextResponseDTO textGenerateRaw(final String prompt) throws IOException {
        return generateTextResponse(prompt, true);
    }

    private AIVelocityTextResponseDTO generateTextResponse(String prompt, boolean raw) throws IOException {
        final Optional<AppConfig> config = ConfigService.INSTANCE.config(null);

        if (config.isPresent()) {
            try {
                ChatGPTTextService service = new ChatGPTTextServiceImpl(config.get());
                AITextResponseDTO resp = service.sendChatGPTRequest(prompt, config, raw);
                return new AIVelocityTextResponseDTO(200, resp.getPrompt(), resp.getResponse());
            } catch (Exception e) {
                return new AIVelocityTextResponseDTO(500, prompt, e.getMessage());
            }
        } else {
            return new AIVelocityTextResponseDTO(500, prompt, "Configuration missing");
        }
    }

    public AIVelocityImageResponseDTO imageGenerate(String prompt) {
        return processImageRequest(prompt, false);
    }

    public AIVelocityImageResponseDTO imageGenerateRaw(String prompt) {
        return processImageRequest(prompt, true);
    }

    private AIVelocityImageResponseDTO processImageRequest(String prompt, boolean isRaw) {
        final Optional<AppConfig> config = ConfigService.INSTANCE.config(null);

        if (config.isPresent()) {
            try {
                ChatGPTImageService service = new ChatGPTImageServiceImpl(config.get());
                AIImageResponseDTO aiImageResponseDTO = service.sendChatGPTRequest(prompt, config, isRaw);
                return new AIVelocityImageResponseDTO(Integer.valueOf(aiImageResponseDTO.getChatGPTResponseStatus()), aiImageResponseDTO.getPrompt(),
                    aiImageResponseDTO.getImageUrl());
//                final TempFileAPI tempApi = APILocator.getTempFileAPI();
//                DotTempFile file = tempApi.createTempFileFromUrl("MyImage", null, new URL(aiImageResponseDTO.getImageUrl()), 30, 1000);
//                return file;
            } catch (Exception e) {
                return new AIVelocityImageResponseDTO(500, prompt, e.getMessage());
            }
        } else {
            return new AIVelocityImageResponseDTO(500, prompt, "Configuration missing");
        }
    }
}
