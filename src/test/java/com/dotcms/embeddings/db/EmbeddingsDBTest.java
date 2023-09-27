package com.dotcms.embeddings.db;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingsDBTest {

    @BeforeEach
    void setUp() {




    }

    @Test
    void test_initVectorExtension() throws DotDataException {
        DotConnect db = new DotConnect();
        db.setSQL(" DROP EXTENSION if exists vector").loadResult();


    }

    @Test
    void initVectorDbTable() {
    }

    @Test
    void runSQL() {
    }

    @Test
    void countEmbeddings() {
    }
}
