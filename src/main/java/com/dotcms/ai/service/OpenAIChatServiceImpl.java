package com.dotcms.ai.service;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;

import java.util.List;
import java.util.Map;

public class OpenAIChatServiceImpl implements OpenAIChatService {
    final AppConfig config;

    public OpenAIChatServiceImpl(AppConfig appConfig) {

        this.config=appConfig;
    }

    public JSONObject sendTextPrompt(String prompt) {
        JSONObject request = new JSONObject();
        request.putIfAbsent("model",config.getModel());
        request.putIfAbsent("temperature",config.getConfigFloat(AppKeys.COMPLETION_TEMPERATURE));
        if(UtilMethods.isEmpty(request.optString("messages"))){
            List messages = List.of(
                    Map.of("role","system", "content", config.getRolePrompt()),
                    Map.of("role","user", "content", prompt)
            );
            request.put("messages", messages);

        }
        return new JSONObject(OpenAIRequest.doRequest(config.getApiUrl(),"POST",config.getApiKey(),request)) ;
    }
}
