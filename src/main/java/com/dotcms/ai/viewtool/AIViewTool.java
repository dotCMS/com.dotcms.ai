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

    public String textGenerate(String prompt) throws IOException {
        return generateTextResponse(prompt, false);
    }

    public String textGenerateRaw(String prompt) throws IOException {
        return generateTextResponse(prompt, true);
    }

    private String generateTextResponse(String prompt, boolean raw) throws IOException {
        final Optional<AppConfig> config = ConfigService.INSTANCE.config(null);

        if (config.isPresent()) {
            try {
                ChatGPTService service = new ChatGPTServiceImpl(config.get());
                AITextResponseDTO resp = service.sendChatGPTRequest(prompt, config, raw);
                AIVelocityTextResponseDTO velocityResponse = new AIVelocityTextResponseDTO(200, resp.getResponse(), resp.getPrompt());
                return Marshaller.marshal(velocityResponse);
            } catch (Exception e) {
                AIVelocityTextResponseDTO velocityResponse = new AIVelocityTextResponseDTO(500, e.getMessage(), prompt);
                return Marshaller.marshal(velocityResponse);
            }
        } else {
            AIVelocityTextResponseDTO velocityResponse = new AIVelocityTextResponseDTO(500, "Configuration missing", prompt);
            return Marshaller.marshal(velocityResponse);
        }
    }

    public String imageGenerate(String prompt) {
        return "Not implemented";
    }

    public String imageGenerateRaw(String prompt) {
        return "Not implemented";
    }

}
