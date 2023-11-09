package com.dotcms.ai.api;

import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.Tuple2;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public interface EmbeddingsAPI {


    static EmbeddingsAPI impl(Host host) {
        return new EmbeddingsAPIImpl(host);
    }

    static EmbeddingsAPI impl() {
        return new EmbeddingsAPIImpl(null);
    }

    void shutdown();

    boolean generateEmbeddingsforContent(Contentlet contentlet, String index);

    boolean generateEmbeddingsforContent(Contentlet contentlet, List<Field> fields, String index);

    boolean generateEmbeddingsforContent(@NotNull Contentlet contentlet, String velocityTemplate, String indexName);

    int deleteEmbedding(EmbeddingsDTO dto);


    JSONObject reduceChunksToContent(EmbeddingsDTO searcher, List<EmbeddingsDTO> searchResults);

    JSONObject searchForContent(EmbeddingsDTO searcher);

    List<EmbeddingsDTO> getEmbeddingResults(EmbeddingsDTO searcher);

    long countEmbeddings(EmbeddingsDTO searcher);

    Map<String, Map<String, Object>> countEmbeddingsByIndex();

    void dropEmbeddingsTable();

    void initEmbeddingsTable();

    /**
     * Returns
     *
     * @param content
     * @return
     */
    Tuple2<Integer, List<Float>> pullOrGenerateEmbeddings(String content);
}
