package com.dotcms.ai.viewtool;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.service.OpenAIChatService;
import com.dotcms.ai.service.OpenAIChatServiceImpl;
import com.dotcms.ai.service.OpenAIImageService;
import com.dotcms.ai.service.OpenAIImageServiceImpl;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.json.JSONObject;
import java.io.IOException;
import java.util.Map;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

public class AIViewTool implements ViewTool {

    AppConfig config;
    private ViewContext context;

    @Override
    public void init(Object obj) {
        context = (ViewContext) obj;
        this.config = ConfigService.INSTANCE.config(
                WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(context.getRequest()));


    }

    /**
     * Generate a response from the AI prompt service with adding config data to original prompt (rolePrompt,
     * textPrompt, imagePrompt)
     *
     * @return JSONObject
     */
    public JSONObject generateText(final String prompt) throws IOException {
        return generateText(prompt, false);
    }

    /**
     * Processes image request by calling TextService.
     *
     * @param prompt
     * @param raw
     * @return
     * @throws IOException
     */
    private JSONObject generateText(String prompt, boolean raw) throws IOException {

        OpenAIChatService service = new OpenAIChatServiceImpl(config);
        return raw ? service.sendRawRequest(new JSONObject(prompt)) : service.sendTextPrompt(prompt);


    }

    public JSONObject generateText(final JSONObject prompt) throws IOException {

        OpenAIChatService service = new OpenAIChatServiceImpl(config);
        return service.sendRawRequest(prompt);
    }

    /**
     * Processes image request by calling ImageService. If response is OK creates temp file and adds its name in
     * response
     *
     * @param prompt
     * @return
     */
    private JSONObject generateImage(String prompt) {

        OpenAIImageService service = new OpenAIImageServiceImpl(config);
        try {

            return service.sendTextPrompt(prompt);

        } catch (Exception e) {
            final JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("response", e.getMessage());
            return jsonResponse;
        }
    }

    private JSONObject generateImage(Map prompt) {

        OpenAIImageService service = new OpenAIImageServiceImpl(config);
        try {

            return service.sendRequest(new JSONObject(prompt));

        } catch (Exception e) {
            final JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("response", e.getMessage());
            return jsonResponse;
        }
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
