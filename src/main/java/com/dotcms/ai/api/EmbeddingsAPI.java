package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EmbeddingsAPI {




    static EmbeddingsAPI impl(Host host) {
        return new EmbeddingsAPIImpl(host);
    }

    static EmbeddingsAPI impl() {
        return new EmbeddingsAPIImpl(null);
    }

    void shutdown();

    void generateEmbeddingsforContent(Contentlet contentlet, String index);

    int deleteEmbedding(EmbeddingsDTO dto);

    void generateEmbeddingsforContent(Contentlet contentlet, Optional<Field> field, String index);

    List<Float> generateEmbeddingsforString(String stringToEncode);

    JSONObject searchEmbedding(EmbeddingsDTO searcher);

    List<EmbeddingsDTO> getEmbeddingResults(EmbeddingsDTO searcher);

    Map<String,Long> countEmbeddings(EmbeddingsDTO searcher);

    Map<String,Long> countEmbeddingsByIndex();

    void dropEmbeddingsTable();

    void initEmbeddingsTable();
}
