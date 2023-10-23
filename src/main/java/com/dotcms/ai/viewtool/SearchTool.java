package com.dotcms.ai.viewtool;

import com.dotcms.ai.api.ContentToStringUtil;
import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

public class SearchTool implements ViewTool {

    final private HttpServletRequest request;
    final private Host host;
    final private AppConfig app;

    /**
     * $ai.search
     *
     * @param initData
     */
    SearchTool(Object initData) {
        this.request = ((ViewContext) initData).getRequest();
        this.host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(this.request);
        this.app = ConfigService.INSTANCE.config(this.host);
    }


    @Override
    public void init(Object initData) {
        /* unneeded because of constructor */
    }


    public JSONObject query(Map<String, Object> mapIn) {
        User user = PortalUtil.getUser(request);
        EmbeddingsDTO searcher = EmbeddingsDTO.from(mapIn).withUser(user).build();


        return EmbeddingsAPI.impl(host).searchForContent(searcher);
    }

    public JSONObject query(String query) {

        return query(query, "default");
    }

    public JSONObject query(String query, String indexName) {
        User user = PortalUtil.getUser(request);

        EmbeddingsDTO searcher = new EmbeddingsDTO.Builder().withQuery(query).withIndexName(indexName).withUser(user).withLimit(50).withThreshold(.25f).build();


        return EmbeddingsAPI.impl(host).searchForContent(searcher);

    }

    public JSONObject related(ContentMap contentMap, String indexName) {

        return related(contentMap.getContentObject(), indexName);

    }

    public JSONObject related(Contentlet contentlet, String indexName) {
        User user = PortalUtil.getUser(request);
        Optional<Field> field = ContentToStringUtil.impl.get().guessWhatFieldToIndex(contentlet);


        Optional<String> contentToRelate = ContentToStringUtil.impl.get().parseField(contentlet, field);
        if (contentToRelate.isEmpty()) {
            return new JSONObject();
        }
        EmbeddingsDTO searcher = new EmbeddingsDTO.Builder().withQuery(contentToRelate.get()).withIndexName(indexName).withExcludeIndentifiers(new String[]{contentlet.getIdentifier()}).withUser(user).withLimit(50).withThreshold(.25f).build();
        return EmbeddingsAPI.impl(host).searchForContent(searcher);


    }

}
