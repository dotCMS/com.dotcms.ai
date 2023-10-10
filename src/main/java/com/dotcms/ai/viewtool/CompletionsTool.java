package com.dotcms.ai.viewtool;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class CompletionsTool implements ViewTool {

    final private HttpServletRequest request;
    final private Host host;
    final private AppConfig app;
    CompletionsTool(Object initData) {
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
                this.app.getConfig(AppKeys.COMPLETION_ROLE_PROMPT, "You concisely answer questions based on text that is provided to you."),
                AppKeys.COMPLETION_TEXT_PROMPT.key,
                this.app.getConfig(AppKeys.COMPLETION_TEXT_PROMPT, "Answer this question\\n\\\"${query}?\\\"\\n\\nby summarizing the following text:\\n\\n${supportingText}"),
                AppKeys.COMPLETION_MODEL.key,
                this.app.getConfig(AppKeys.COMPLETION_MODEL, "gpt-3.5-turbo")
        );

    }


}
