package com.dotcms.embeddings.api;


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


        List<Double> embeddings = new EmbeddingsAPIImpl().generateEmbeddingsforString(testPrompt);


        assert(embeddings.size()>100);
        Double embed0= embeddings.get(0);
        Double embed1= embeddings.get(1);
        Assertions.assertEquals(embedding0,embed0);
        Assertions.assertEquals(embedding1,embed1);


    }







}
