package com.dotcms.ai.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StopwordsUtilTest {



    @Test
    void test_stopwords_works() {
        String testWords = "Do the stopwords work like this";

        String response = new StopwordsUtil().removeStopwords(testWords);

        assertEquals( "stopwords work", response);


    }






}
