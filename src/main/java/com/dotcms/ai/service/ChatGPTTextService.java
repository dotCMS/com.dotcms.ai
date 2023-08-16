package com.dotcms.ai.service;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.model.AITextResponseDTO;
import java.util.Optional;

public interface ChatGPTTextService {

    AITextResponseDTO sendChatGPTRequest(String prompt, Optional<AppConfig> config, boolean isRawPrompt);

}
