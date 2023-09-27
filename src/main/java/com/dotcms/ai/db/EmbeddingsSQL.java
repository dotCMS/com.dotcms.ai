package com.dotcms.ai.db;

public class EmbeddingsSQL {

    private EmbeddingsSQL(){}



    static final String INIT_VECTOR_EXTENSION = "CREATE EXTENSION if not exists vector";


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
                    "content_type varchar(255) not null," +
                    "field_var varchar(255) not null,   " +
                    "title varchar(512) not null,       " +
                    "extracted_text text,               " +
                    "embeddings vector(1536)            " +
                    ");  ";

    public static final String[] CREATE_EMBEDDINGS_INDEXES = {
            "create unique index if not exists dot_embeddings_idx1 on dot_embeddings(inode,content_type,field_var)",
            "create unique index if not exists dot_embeddings_idx2 on dot_embeddings(identifier,language)",
    };

    /**
     * The number of lists in this index should be determined
     * 1. when there is data in the table and
     * 2. should be calculated as (number of rows / 1000)
     */
    public static final String[] CREATE_EMBEDDINGS_IVFFLAT_INDEX = {
            "CREATE INDEX if not exists dot_embeddings_idx3 ON dot_embeddings USING ivfflat (embeddings vector_cosine_ops) WITH (lists = ?);"
    };



    static final String COUNT_EMBEDDINGS = "select count(*) as test from dot_embeddings";




    public static final String INSERT_EMBEDDINGS = "insert into dot_embeddings (inode, identifier,language, content_type, field_var, title, extracted_text, embeddings) values (?,?,?,?,?,?,?,?)";

    public static final String SELECT_EMBEDDINGS_BY_COSINE_DISTANCE = "select inode, title, language, identifier, content_type,field_var from dot_embeddings where content_type like ? and field_var like ? order by embeddings <=> ? limit 10" ;



    static final String DELETE_FROM_EMBEDDINGS_BY_ID = "delete from dot_embeddings where id =?";

    static final String DELETE_FROM_EMBEDDINGS_BY_INODE= "delete from dot_embeddings where inode =?";

    static final String DELETE_FROM_EMBEDDINGS_BY_IDENTIFIER= "delete from dot_embeddings where identifier =? and language=?";

    static final String DELETE_FROM_EMBEDDINGS_BY_CONTENT_TYPE= "delete from dot_embeddings where content_type =?";

    static final String DELETE_FROM_EMBEDDINGS_BY_CONTENT_TYPE_FIELD_VAR= "delete from dot_embeddings where content_type =? and field_var=?";


}
