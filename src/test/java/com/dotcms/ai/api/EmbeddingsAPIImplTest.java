package com.dotcms.ai.api;


import com.dotcms.ai.api.EmbeddingsAPIImpl;
import com.dotcms.ai.app.AppConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


import java.util.Optional;

class EmbeddingsAPIImplTest {

    float embedding0=0.002253932f;
    float embedding1=-0.009333183f;
    final String testPrompt = "The food was delicious and the waiter...";


    @Test
    public void test_embeddings() {

        AppConfig config = new AppConfig(null, null, System.getenv("DOT_OPEN_AI_API_KEY"), null, null, null, null, null, null, null);
        List<Float> embeddings = new EmbeddingsAPIImpl(config).generateEmbeddingsforString(testPrompt);


        assert(embeddings.size()>100);
        Float embed0= embeddings.get(0);
        Float embed1= embeddings.get(1);


        System.out.println("embed0:" + embed0);

        Assertions.assertEquals(embedding0,embed0);
        Assertions.assertEquals(embedding1,embed1);


    }







}
