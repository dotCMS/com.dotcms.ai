package com.dotcms.ai.api;


import com.dotcms.ai.api.EmbeddingsAPIImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


import java.util.Optional;

class EmbeddingsAPIImplTest {

    double embedding0=0.002253932d;
    double embedding1=-0.009333183d;
    final String testPrompt = "The food was delicious and the waiter...";


    @Test
    public void test_embeddings() {


        List<Double> embeddings = new EmbeddingsAPIImpl().generateEmbeddingsforString(testPrompt);


        assert(embeddings.size()>100);
        Double embed0= embeddings.get(0);
        Double embed1= embeddings.get(1);


        System.out.println("embed0:" + embed0);

        Assertions.assertEquals(embedding0,embed0);
        Assertions.assertEquals(embedding1,embed1);


    }







}
