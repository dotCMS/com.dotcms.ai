package com.dotcms.ai.viewtool;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;

public class SearchTool implements ViewTool {

    final private HttpServletRequest request;
    final private Host host;
    final private AppConfig app;

    SearchTool(Object initData) {
        this.request = ((ViewContext) initData).getRequest();
        this.host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(this.request);
        this.app = ConfigService.INSTANCE.config(this.host);
    }


    @Override
    public void init(Object initData) {
        /* unneeded because of constructor */
    }

    EmbeddingsDTO.Builder getSearcher() {
        return new EmbeddingsDTO.Builder();
    }

    public JSONObject search(EmbeddingsDTO.Builder searcher) {
        return this.search(searcher.build());
    }

    public JSONObject search(EmbeddingsDTO searcher) {

        return EmbeddingsAPI.impl(host).searchEmbedding(searcher);
    }

    public JSONObject search(String prompt) {

        return search(prompt, "default");
    }

    public JSONObject search(String prompt, String indexName) {
        User user = PortalUtil.getUser(request);

        EmbeddingsDTO searcher = new EmbeddingsDTO.Builder().withQuery(prompt).withIndexName(indexName).withUser(user).build();

        return EmbeddingsAPI.impl(host).searchEmbedding(searcher);
    }
}
