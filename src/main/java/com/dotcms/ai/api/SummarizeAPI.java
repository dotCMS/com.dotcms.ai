package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.Lazy;

import java.io.InputStream;
import java.io.OutputStream;

public interface SummarizeAPI {

    static SummarizeAPI impl(AppConfig config) {
        return new SummarizeAPIImpl(Lazy.of(()->config));
    }
    static SummarizeAPI impl() {
        return new SummarizeAPIImpl(null);
    }


    JSONObject summarize(String query, String docText,  int resposeLengthInTokens);

     void summarizeStream(String query, String docText, int resposeLengthInTokens, OutputStream out);
}
