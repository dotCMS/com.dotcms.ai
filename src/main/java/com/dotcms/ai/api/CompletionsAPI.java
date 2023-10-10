package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.Lazy;

import java.io.OutputStream;

public interface CompletionsAPI {

    static CompletionsAPI impl(AppConfig config) {
        return new CompletionsAPIImpl(Lazy.of(() -> config));
    }

    static CompletionsAPI impl() {
        return new CompletionsAPIImpl(null);
    }


    JSONObject summarize(CompletionsForm searcher);

    void summarizeStream(CompletionsForm searcher, OutputStream out);
}
