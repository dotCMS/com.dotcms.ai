package com.dotcms.ai.viewtool;

import com.dotcms.ai.api.CompletionsAPI;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class CompletionsTool implements ViewTool {

    final private HttpServletRequest request;
    final private Host host;
    final private AppConfig app;
    final private ViewContext context;


    /**
     * $ai.completions
     *
     * @param initData
     */
    CompletionsTool(Object initData) {
        this.context = (ViewContext) initData;
        this.request = ((ViewContext) initData).getRequest();
        this.host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(this.request);
        this.app = ConfigService.INSTANCE.config(this.host);

    }

    @Override
    public void init(Object initData) {
        // unneeded because of constructor
    }


    public Map<String, String> getConfig() {
        return Map.of(
                AppKeys.COMPLETION_ROLE_PROMPT.key,
                this.app.getConfig(AppKeys.COMPLETION_ROLE_PROMPT),
                AppKeys.COMPLETION_TEXT_PROMPT.key,
                this.app.getConfig(AppKeys.COMPLETION_TEXT_PROMPT),
                AppKeys.COMPLETION_MODEL.key,
                this.app.getConfig(AppKeys.COMPLETION_MODEL)
        );

    }


    public JSONObject summarize(String prompt, String indexName) {
        CompletionsForm form = new CompletionsForm.Builder().indexName(indexName).query(prompt).build();
        return CompletionsAPI.impl().summarize(form);
    }

    public JSONObject summarize(String prompt) {
        return summarize(prompt, "default");
    }
    /**
     * Note this does not really stream the output as Velocity Buffers the response.
     *
     * @param prompt
     * @param indexName
     * @return
     */
    public void stream(String prompt, String indexName) {
        CompletionsForm form = new CompletionsForm.Builder().stream(true).indexName(indexName).query(prompt).build();
        try {
            CompletionsAPI.impl().summarizeStream(form, this.context.getResponse().getOutputStream());
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), e);
        }
    }

    public JSONObject prompt(Map<String, Object> prompt) {
        JSONObject json = new JSONObject(prompt);
        return CompletionsAPI.impl().raw(json);
    }


}
