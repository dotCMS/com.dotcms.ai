package com.dotcms.embeddings.api;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

public interface EmbeddingsAPI {

    static EmbeddingsAPI impl() {
        return new EmbeddingsAPIImpl();
    }

    void shutdown();

    void generateEmbeddingsforContentAndField(Contentlet contentlet, Field field);
}
