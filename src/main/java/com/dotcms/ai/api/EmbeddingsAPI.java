package com.dotcms.ai.api;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.List;
import java.util.Optional;

public interface EmbeddingsAPI {

    static EmbeddingsAPI impl() {
        return new EmbeddingsAPIImpl();
    }

    void shutdown();

    void generateEmbeddingsforContent(Contentlet contentlet);

    void generateEmbeddingsforContent(Contentlet contentlet, Optional<Field> field);

    List<Double> generateEmbeddingsforString(String stringToEncode);
}
