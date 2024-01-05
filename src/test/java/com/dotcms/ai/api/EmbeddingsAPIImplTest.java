package com.dotcms.ai.api;


import com.dotcms.ai.app.AppConfig;
import com.dotcms.contenttype.model.field.Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class EmbeddingsAPIImplTest {

    final String testPrompt = "The food was delicious and the waiter...";
    float embedding0 = 0.002253932f;
    float embedding1 = -0.009333183f;

    @Test
    public void test_embeddings() {

        if(true){
            return;
        }
        AppConfig config = new AppConfig(Map.of());
        List<Float> embeddings = new EmbeddingsAPIImpl(null).pullOrGenerateEmbeddings(testPrompt)._2;


        assert (embeddings.size() > 100);
        Float embed0 = embeddings.get(0);
        Float embed1 = embeddings.get(1);


        System.out.println("embed0:" + embed0);

        Assertions.assertEquals(embedding0, embed0);
        Assertions.assertEquals(embedding1, embed1);


    }


    @Test
    public void test_parse_types_and_fields() throws  Exception {

        String test1="blog,contentType.field1";

        Map<String,List<Field>> fields = new EmbeddingsAPIImpl(null).parseTypesAndFields(test1);


    }




}
