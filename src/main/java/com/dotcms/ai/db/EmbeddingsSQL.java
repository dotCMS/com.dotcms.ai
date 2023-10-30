package com.dotcms.ai.db;

class EmbeddingsSQL {

    static final String INIT_VECTOR_EXTENSION = "create extension if not exists vector with schema public;";
    static final String DROP_EMBEDDINGS_TABLE = "drop table if exists dot_embeddings";
    /**
     * Our embeddings column has 1536 dimentions because that is the number of dimentions returned by
     * OpenAI text-embedding-ada-002.
     * <p>
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
                    "index_name varchar(255) not null default 'default',   " +
                    "title varchar(512) not null,       " +
                    "extracted_text text,               " +
                    "extracted_text_hash varchar(255),  " +
                    "token_count int ,                  " +
                    "embeddings vector(1536)            " +
                    ");  ";

    static final String[] CREATE_TABLE_INDEXES = {
            "create index if not exists dot_embeddings_idx_index_name on dot_embeddings(lower(index_name))",
            "create index if not exists dot_embeddings_idx_inode on dot_embeddings(inode)",
            "create index if not exists dot_embeddings_idx_type_field on dot_embeddings(inode,lower(content_type),lower(field_var))",
            "create index if not exists dot_embeddings_idx_id_lang on dot_embeddings(identifier,language)",
            "create index if not exists dot_embeddings_idx_host on dot_embeddings(host)",
            "create index if not exists dot_embeddings_idx_text_hash on dot_embeddings(extracted_text_hash)",
            "create index if not exists dot_embeddings_idx_vector ON dot_embeddings USING hnsw (embeddings vector_cosine_ops);"
    };
    /**
     * The number of lists in this index should be determined
     * 1. when there is data in the table and
     * 2. should be calculated as (number of rows / 1000)
     */
    static final String[] CREATE_EMBEDDINGS_IVFFLAT_INDEX = {
            "CREATE INDEX if not exists dot_embeddings_idx3 ON dot_embeddings USING ivfflat (embeddings vector_cosine_ops) WITH (lists = ?);"
    };


    static final String SELECT_EMBEDDING_BY_TEXT_HASH =
            "Select token_count, embeddings from dot_embeddings where extracted_text_hash=? limit 1";


    static final String INSERT_EMBEDDINGS =
            "insert into dot_embeddings " +
            "   (" +
                    "inode, " +
                    "identifier," +
                    "language, " +
                    "content_type, " +
                    "field_var, " +
                    "title, " +
                    "extracted_text, " +
                    "extracted_text_hash, " +
                    "host, " +
                    "index_name, " +
                    "token_count, " +
                    "embeddings) " +
                "values (" +
                    "?," +
                    "?," +
                    "?," +
                    "?," +
                    "?," +
                    "?," +
                    "?," +
                    "?," +
                    "?," +
                    "?," +
                    "?," +
                    "?)";


    static final String SEARCH_EMBEDDINGS_SELECT_PREFIX=
            "select " +
            "inode, title, language, identifier,host, content_type, extracted_text ,field_var, index_name, distance, token_count " +
            "from (select inode, title, language, identifier,host, content_type,extracted_text, field_var,index_name, token_count, (embeddings {operator} ?) AS distance " +
            "from dot_embeddings where true ";

    static final String COUNT_EMBEDDINGS_PREFIX=
            "select count(distinct inode) as test " +
                    "from (select inode, title, language, identifier,host, content_type,extracted_text, field_var,index_name, token_count, (embeddings {operator} ?) AS distance " +
                    "from dot_embeddings where true ";


    static final String COUNT_EMBEDDINGS_BY_INDEX=
            "select " +
                "embeddings, " +
                "index_name, " +
                "contents, " +
                "token_total " +
            "from (" +
                "select " +
                    "count(*) as embeddings, " +
                    "index_name, " +
                    "count(distinct(inode)) as contents, " +
                    "sum(token_count) as token_total " +
                "from " +
                    "dot_embeddings  " +
                "group by " +
                    "index_name " +
                "order by " +
                    "index_name " +
            ") data";

    private EmbeddingsSQL() {
    }


}
