package com.dotcms.ai.viewtool;

import com.dotcms.ai.ChatGPTService;
import com.dotcms.ai.ChatGPTServiceImpl;
import com.dotcms.ai.Marshaller;
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
     * @param prompt
     * @return AIVelocityTextResponseDTO
     * @throws IOException
     */
    public AIVelocityTextResponseDTO textGenerate(final String prompt) throws IOException {
        return generateTextResponse(prompt, false);
    }

    /**
     * Generate a raw response from the AI prompt service w/o adding data from config to prompt
     * @param prompt
     * @return AIVelocityTextResponseDTO
     * @throws IOException
     */
    public AIVelocityTextResponseDTO textGenerateRaw(final String prompt) throws IOException {
        return generateTextResponse(prompt, true);
    }

    private AIVelocityTextResponseDTO generateTextResponse(String prompt, boolean raw) throws IOException {
        final Optional<AppConfig> config = ConfigService.INSTANCE.config(null);

        if (config.isPresent()) {
            try {
                ChatGPTService service = new ChatGPTServiceImpl(config.get());
                AITextResponseDTO resp = service.sendChatGPTRequest(prompt, config, raw);
                return new AIVelocityTextResponseDTO(200, resp.getResponse(), resp.getPrompt());
            } catch (Exception e) {
                return new AIVelocityTextResponseDTO(500, e.getMessage(), prompt);
            }
        } else {
            return new AIVelocityTextResponseDTO(500, "Configuration missing", prompt);
        }
    }

    public String imageGenerate(String prompt) {
        return "Not implemented";
    }

    public String imageGenerateRaw(String prompt) {
        return "Not implemented";
    }
}
