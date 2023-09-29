package com.dotcms.ai.db;

class EmbeddingsSQL {

    private EmbeddingsSQL(){}



    static final String INIT_VECTOR_EXTENSION = "create extension if not exists vector with schema public;";


    /**
     * Our embeddings column has 1536 dimentions because that is the number of dimentions returned by
     * OpenAI text-embedding-ada-002.
     *
     * The dimensions would change if we used a different model.
     */
    static final String CREATE_EMBEDDINGS_TABLE =
            "create table if not exists dot_embeddings( " +
                    "id bigserial primary key,          " +
                    "inode varchar(255) not null,       " +
                    "identifier varchar(255) not null,  " +
                    "language bigint not null,          " +
                    "host varchar(255) not null,        " +
                    "content_type varchar(255) not null," +
                    "field_var varchar(255) not null,   " +
                    "title varchar(512) not null,       " +
                    "extracted_text text,               " +
                    "embeddings vector(1536)            " +
                    ");  ";

    static final String[] CREATE_EMBEDDINGS_INDEXES = {
            "create unique index if not exists dot_embeddings_type_field_idx on dot_embeddings(inode,content_type,field_var)",
            "create index if not exists dot_embeddings_id_lang_idx on dot_embeddings(identifier,language)",
            "create index if not exists dot_embeddings_host_idx on dot_embeddings(identifier,language)",
            "create index if not exists dot_embeddings_host_idx on dot_embeddings(host)",
    };

    /**
     * The number of lists in this index should be determined
     * 1. when there is data in the table and
     * 2. should be calculated as (number of rows / 1000)
     */
    static final String[] CREATE_EMBEDDINGS_IVFFLAT_INDEX = {
            "CREATE INDEX if not exists dot_embeddings_idx3 ON dot_embeddings USING ivfflat (embeddings vector_cosine_ops) WITH (lists = ?);"
    };



    static final String COUNT_EMBEDDINGS = "select count(*) as test from dot_embeddings";




    static final String INSERT_EMBEDDINGS = "insert into dot_embeddings (inode, identifier,language, content_type, field_var, title, extracted_text, host, embeddings) values (?,?,?,?,?,?,?,?,?)";


}
