package com.dotcms.embeddings.viewtool;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

import com.dotcms.embeddings.util.Constants;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotcms.security.apps.AppSecrets;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import io.vavr.Lazy;
import io.vavr.control.Try;

public class EmbeddingsTool implements ViewTool {

    private HttpServletRequest request;
    private Host host;

    private final Lazy<Optional<AppSecrets>> appSecrets = Lazy.of(()->
                Try.of(() ->
                    APILocator.getAppsAPI().getSecrets(Constants.EMBEDDINGS_APP_KEY, true, host, APILocator.systemUser()))
                .getOrElse(Optional.empty()));

	@Override
    public void init(Object initData) {
        this.request = ((ViewContext) initData).getRequest();
        this.host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(this.request);
	}

	/**
	 * Creates the full url where the file lives in the cdn.
	 *
	 * @return the cdn domain + url, that is where is file lives
	 */
	public String cdnify(final String fileUrl) {
	    if(!appSecrets.get().isPresent() || fileUrl==null || fileUrl.contains("//") || !fileUrl.startsWith("/")) {
	        return fileUrl;
	    }

		return null;


	}











}
