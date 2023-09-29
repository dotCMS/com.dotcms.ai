package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.List;
import java.util.Optional;

public interface EmbeddingsAPI {

    static EmbeddingsAPI impl(AppConfig config) {
        return new EmbeddingsAPIImpl(config);
    }
    static EmbeddingsAPI impl() {
        return new EmbeddingsAPIImpl(null);
    }
    void shutdown();

    void generateEmbeddingsforContent(Contentlet contentlet);

    int deleteEmbedding(EmbeddingsDTO dto);

    void generateEmbeddingsforContent(Contentlet contentlet, Optional<Field> field);

    List<Float> generateEmbeddingsforString(String stringToEncode);
}
