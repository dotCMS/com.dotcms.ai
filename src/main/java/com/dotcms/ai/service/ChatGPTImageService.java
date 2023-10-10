package com.dotcms.ai.service;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.model.AIImageResponseDTO;
import java.util.Optional;

public interface ChatGPTImageService {

    AIImageResponseDTO sendChatGPTRequest(String prompt, AppConfig config, boolean isRawPrompt);

}
