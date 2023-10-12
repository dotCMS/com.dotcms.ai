package com.dotcms.ai.util;

import com.dotcms.repackage.org.directwebremoting.json.types.JsonObject;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenAIRequestTest {





    @Test
    void doRequest() {
    }

    @Test
    void testDoRequest() {
    }

    @Test
    public void streamRequest() throws Exception {

        String url="https://api.openai.com/v1/chat/completions";
        String apiKey = System.getenv("OPEN_AI_API_KEY");
        assertTrue(UtilMethods.isSet(apiKey));

        JSONObject data = new JSONObject();
        JSONArray messages = new JSONArray();
        data.put("model", "gpt-3.5-turbo");
        data.put("stream", true);
        data.put("temperature",0.5);
        data.put("max_tokens", 2048);
        messages.add(Map.of("role", "user", "content", "please tell me a short story"));
        messages.add(Map.of("role", "system", "content", "you are a caretaker putting a child to bed"));

        data.put("messages", messages);

        OpenAIRequest.doPost(url,  apiKey, data, System.out);



    }
}
