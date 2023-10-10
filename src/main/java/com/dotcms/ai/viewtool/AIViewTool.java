package com.dotcms.ai.viewtool;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.model.AIImageResponseDTO;
import com.dotcms.ai.model.AITextResponseDTO;
import com.dotcms.ai.model.AIVelocityImageResponseDTO;
import com.dotcms.ai.model.AIVelocityTextResponseDTO;
import com.dotcms.ai.service.ChatGPTImageService;
import com.dotcms.ai.service.ChatGPTImageServiceImpl;
import com.dotcms.ai.service.ChatGPTTextService;
import com.dotcms.ai.service.ChatGPTTextServiceImpl;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;

public class AIViewTool implements ViewTool {
    private ViewContext context ;


    @Override
    public void init(Object obj) {
        context = (ViewContext) obj;
    }


    HttpServletRequest getRequest(){
        return context.getRequest();
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
     * Processes image request by calling TextService.
     *
     * @param prompt
     * @param raw
     * @return
     * @throws IOException
     */
    private AIVelocityTextResponseDTO generateTextResponse(String prompt, boolean raw) throws IOException {
        final AppConfig config = ConfigService.INSTANCE.config();


        try {
            ChatGPTTextService service = new ChatGPTTextServiceImpl(config);
            AITextResponseDTO resp = service.sendChatGPTRequest(prompt,  raw);
            return new AIVelocityTextResponseDTO(resp.getModel(), "200", resp.getPrompt(), resp.getResponse());
        } catch (Exception e) {
            return new AIVelocityTextResponseDTO(null, "500", prompt, e.getMessage());
        }

    }

    /**
     * Generate a raw response from the AI prompt service w/o adding data from config to prompt
     *
     * @return AIVelocityTextResponseDTO
     */
    public AIVelocityTextResponseDTO textGenerateRaw(final String prompt) throws IOException {
        return generateTextResponse(prompt, true);
    }

    /**
     * Generate a response from the AI image service with adding config data to original prompt (imagePrompt). Image size is set in configuration file. Temp
     * File id is being returned in response
     *
     * @return AIVelocityImageResponseDTO
     */
    public AIVelocityImageResponseDTO imageGenerate(String prompt) {
        return processImageRequest(prompt, false);
    }

    /**
     * Processes image request by calling ImageService. If response is OK creates temp file and adds its name in response
     *
     * @param prompt
     * @param isRaw
     * @return
     */
    private AIVelocityImageResponseDTO processImageRequest(String prompt, boolean isRaw) {
        final AppConfig config = ConfigService.INSTANCE.config();


        try {
            ChatGPTImageService service = new ChatGPTImageServiceImpl(config);
            final AIImageResponseDTO aiImageResponseDTO = service.sendChatGPTRequest(prompt, config, isRaw);
            String fileId = null;
            if (aiImageResponseDTO.getHttpStatus().equals(String.valueOf(HttpResponseStatus.OK.code()))) {
                final TempFileAPI tempApi = APILocator.getTempFileAPI();
                DotTempFile file = tempApi.createTempFileFromUrl("ChatGPTImage", getRequest(), new URL(aiImageResponseDTO.getResponse()), 10, 1000);
                return new AIVelocityImageResponseDTO(aiImageResponseDTO.getModel(), aiImageResponseDTO.getHttpStatus(), prompt,
                        file.id);
            } else {
                return new AIVelocityImageResponseDTO(aiImageResponseDTO.getModel(), aiImageResponseDTO.getHttpStatus(), prompt,
                        aiImageResponseDTO.getResponse());
            }
        } catch (Exception e) {
            return new AIVelocityImageResponseDTO(null, "500", prompt, e.getMessage());
        }
    }

    /**
     * Generate a response from the AI image service  service w/o adding data from config to prompt. Image size is set in configuration file. Temp File id is
     * being returned in response
     *
     * @return AIVelocityImageResponseDTO
     */
    public AIVelocityImageResponseDTO imageGenerateRaw(String prompt) {
        return processImageRequest(prompt, true);
    }

    public EmbeddingsTool getEmbeddings() {
        return new EmbeddingsTool(context);
    }

    public SearchTool getSearch() {
        return new SearchTool(context);
    }
    public CompletionsTool getCompletions() {
        return new CompletionsTool(context);
    }
}
