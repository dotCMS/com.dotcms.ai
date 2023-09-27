package com.dotcms.embeddings.db;


import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmbeddingsDB {



    void initVectorExtension() {
        Logger.info(EmbeddingsDB.class, "Adding PGVector extenstion to database");
        runSQL(EmbeddingsSQL.INIT_VECTOR_EXTENSION);
    }


    void initVectorDbTable() {
        Logger.info(EmbeddingsDB.class, "Adding table dot_embeddings to database");
        runSQL(EmbeddingsSQL.CREATE_EMBEDDINGS_TABLE);

        Logger.info(EmbeddingsDB.class, "Adding indexes to dot_embedding_table");
        for(String index : EmbeddingsSQL.CREATE_EMBEDDINGS_INDEXES) {
            runSQL(index);

        }



    }

    void initVectorDbIndex() {


    }

    void countVectorDbSize() {


    }


    public void runSQL(String sql) {
        try (Connection db = DbConnectionFactory.getDataSource().getConnection()) {
            new DotConnect().setSQL(sql).loadResult();
        } catch (SQLException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }


    public void runSQL(DotConnect dotConnect) {
        try (Connection db = DbConnectionFactory.getDataSource().getConnection()) {
            dotConnect.loadResult(db);
        } catch (SQLException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    public List<Map<String, Object>> loadResults(String sql) {
        try (Connection db = DbConnectionFactory.getDataSource().getConnection()) {
            return new DotConnect().setSQL(sql).loadObjectResults();
        } catch (SQLException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }



    public void saveEmbeddings(ContentEmbeddings embeddings) {
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            Array embeds = conn.createArrayOf("float8", embeddings.embeddings);

            DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL(EmbeddingsSQL.INSERT_EMBEDDINGS)
                    .addParam(embeddings.inode)
                    .addParam(embeddings.identifier)
                    .addParam(embeddings.language)
                    .addParam(embeddings.contentType)
                    .addParam(embeddings.field)
                    .addParam(embeddings.title)
                    .addParam(embeddings.extractedText)
                    .addParam(embeds);

            dotConnect.loadResult(conn);
        } catch (SQLException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }


    public List<ContentEmbeddings> searchEmbeddings(ContentEmbeddings embeddings) {
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            Array embeds = conn.createArrayOf("float8", embeddings.embeddings);
            String contentType = UtilMethods.isSet(embeddings.contentType) ? embeddings.contentType : "%";
            String fieldVar = UtilMethods.isSet(embeddings.field) ? embeddings.field : "%";
            List<ContentEmbeddings> results = new ArrayList<>();


            DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL(EmbeddingsSQL.SELECT_EMBEDDINGS_BY_COSINE_DISTANCE)
                    .addParam(contentType)
                    .addParam(fieldVar)
                    .addParam(embeds);

            dotConnect.loadObjectResults(conn).forEach(r -> {
                ContentEmbeddings conEmbed = new ContentEmbeddings.Builder()
                        .withContentType((String) r.get("content_type"))
                        .withField((String) r.get("field_var"))
                        .withIdentifier((String) r.get("identifier"))
                        .withInode((String) r.get("inode"))
                        .withTitle((String) r.get("title"))
                        .withLanguage((long) r.get("language"))
                        .build();
                results.add(conEmbed);
            });

            return results;
        } catch (SQLException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }



    public void deleteFromEmbeddings(String inode){
        DotConnect dotConnect = new DotConnect();
        if(UtilMethods.isSet(inode)) {

            dotConnect
                    .setSQL(EmbeddingsSQL.DELETE_FROM_EMBEDDINGS_BY_INODE)
                    .addParam(inode);
            runSQL(dotConnect);
        }


    }

    public void deleteFromEmbeddings(String identifier, long language) {
        DotConnect dotConnect = new DotConnect();
        if (UtilMethods.isSet(identifier)) {

            dotConnect
                    .setSQL(EmbeddingsSQL.DELETE_FROM_EMBEDDINGS_BY_IDENTIFIER)
                    .addParam(identifier)
                    .addParam(language);
            runSQL(dotConnect);
        }


    }






    private EmbeddingsDB() {
        initVectorExtension();
        initVectorDbTable();

    }

    long countEmbeddings() {
        return (Long) loadResults(EmbeddingsSQL.COUNT_EMBEDDINGS).get(0).get("test");
    }


    public static Lazy<EmbeddingsDB> instance = Lazy.of(EmbeddingsDB::new);









}
