package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.rest.forms.SummarizeForm;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.Lazy;

import java.io.OutputStream;

public interface SummarizeAPI {

    static SummarizeAPI impl(AppConfig config) {
        return new SummarizeAPIImpl(Lazy.of(() -> config));
    }

    static SummarizeAPI impl() {
        return new SummarizeAPIImpl(null);
    }


    JSONObject summarize(SummarizeForm searcher);

    void summarizeStream(SummarizeForm searcher,  OutputStream out);
}
