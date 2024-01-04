package com.dotcms.ai.service;

import com.dotmarketing.util.json.JSONObject;

public interface OpenAIChatService {

    JSONObject sendTextPrompt(String prompt);

    JSONObject sendRawRequest(JSONObject prompt);

}
