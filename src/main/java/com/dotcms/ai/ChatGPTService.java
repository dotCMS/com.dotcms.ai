package com.dotcms.ai;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.model.AITextResponseDTO;
import java.util.Optional;

public interface ChatGPTService {

    AITextResponseDTO sendChatGPTRequest(String prompt, Optional<AppConfig> config);

}
