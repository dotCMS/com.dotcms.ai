package com.dotcms.ai.db;


import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.pgvector.PGvector;
import io.vavr.Lazy;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        for (String index : EmbeddingsSQL.CREATE_EMBEDDINGS_INDEXES) {
            runSQL(index);

        }


    }

    void initVectorDbIndex() {


    }

    void countVectorDbSize() {


    }


    public void runSQL(String sql) {
        try (Connection db = getPGVectorConnection()) {
            new DotConnect().setSQL(sql).loadResult();
        } catch (SQLException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }


    public void runSQL(DotConnect dotConnect) {
        try (Connection db = getPGVectorConnection()) {
            dotConnect.loadResult(db);
        } catch (SQLException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    public List<Map<String, Object>> loadResults(String sql) {
        try (Connection db = getPGVectorConnection()) {
            return new DotConnect().setSQL(sql).loadObjectResults();
        } catch (SQLException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    private Connection getPGVectorConnection() throws SQLException {
        Connection conn = DbConnectionFactory.getDataSource().getConnection();
        PGvector.addVectorType(conn);
        return conn;
    }


    public void saveEmbeddings(EmbeddingsDTO embeddings) {

        PGvector vector = new PGvector(ArrayUtils.toPrimitive(embeddings.embeddings));
        try (Connection conn = getPGVectorConnection();
             PreparedStatement statement = conn.prepareStatement(EmbeddingsSQL.INSERT_EMBEDDINGS)) {

            int i = 0;
            statement.setObject(++i, embeddings.inode);
            statement.setObject(++i, embeddings.identifier);
            statement.setObject(++i, embeddings.language);
            statement.setObject(++i, embeddings.contentType);
            statement.setObject(++i, embeddings.field);
            statement.setObject(++i, embeddings.title);
            statement.setObject(++i, embeddings.extractedText);
            statement.setObject(++i, embeddings.host);
            statement.setObject(++i, vector);
            statement.execute();

        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }


    public List<EmbeddingsDTO> searchEmbeddings(EmbeddingsDTO dto) {


        StringBuilder sql = new StringBuilder("select " +
                "inode, title, language, identifier,host, content_type,field_var, cosine_similarity " +
                "from (select inode, title, language, identifier,host, content_type,field_var, 1 - (embeddings <=> ?) AS cosine_similarity " +
                "from dot_embeddings where true ");

        List<Object> params = new ArrayList<>();

        params.add(new PGvector(ArrayUtils.toPrimitive(dto.embeddings)));


        if (UtilMethods.isSet(dto.inode)) {
            sql.append(" and inode=? ");
            params.add(dto.inode);
        }
        if (UtilMethods.isSet(dto.identifier)) {
            sql.append(" and identifier=? ");
            params.add(dto.identifier);
        }
        if (dto.language > 0) {
            sql.append(" and language=? ");
            params.add(dto.language);
        }
        if (UtilMethods.isSet(dto.contentType)) {
            sql.append(" and content_type=? ");
            params.add(dto.contentType);
        }
        if (UtilMethods.isSet(dto.field)) {
            sql.append(" and field_var=? ");
            params.add(dto.field);
        }
        if (UtilMethods.isSet(dto.host)) {
            sql.append(" and host=? ");
            params.add(dto.host);
        }

        sql.append(" ) data where cosine_similarity >=? ");
        params.add(dto.threshold);

        sql.append(" order by cosine_similarity desc limit ? offset ? ");
        params.add(dto.limit);
        params.add(dto.offset);




        try (Connection conn = getPGVectorConnection();
             PreparedStatement statement = conn.prepareStatement(sql.toString())) {

            for (int  i = 0; i < params.size(); i++) {
                statement.setObject((i + 1), params.get(i));
            }


            List<EmbeddingsDTO> results = new ArrayList<>();
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                float cosine = rs.getFloat("cosine_similarity");
                EmbeddingsDTO conEmbed = new EmbeddingsDTO.Builder()
                        .withContentType(rs.getString("content_type"))
                        .withField(rs.getString("field_var"))
                        .withIdentifier(rs.getString("identifier"))
                        .withInode(rs.getString("inode"))
                        .withTitle(rs.getString("title"))
                        .withLanguage(rs.getLong("language"))
                        .withHost(rs.getString("host"))
                        .withThreshold(cosine)
                        .build();
                results.add(conEmbed);
            }
            return results;
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }

    }


    public int deleteEmbeddings(EmbeddingsDTO dto) {


        StringBuilder builder = new StringBuilder("delete from dot_embeddings where true ");

        List<Object> params = new ArrayList<>();
        if (UtilMethods.isSet(dto.inode)) {
            builder.append(" and inode=? ");
            params.add(dto.inode);
        }
        if (UtilMethods.isSet(dto.identifier)) {
            builder.append(" and identifier=? ");
            params.add(dto.identifier);
        }
        if (dto.language > 0) {
            builder.append(" and language=? ");
            params.add(dto.language);
        }
        if (UtilMethods.isSet(dto.contentType)) {
            builder.append(" and content_type=? ");
            params.add(dto.contentType);
        }
        if (UtilMethods.isSet(dto.field)) {
            builder.append(" and field_var=? ");
            params.add(dto.field);
        }
        if (UtilMethods.isSet(dto.host)) {
            builder.append(" and host=? ");
            params.add(dto.host);
        }
        Logger.info(EmbeddingsDB.class, "deleting embeddings:" + dto);

        try (Connection conn = getPGVectorConnection();
             PreparedStatement statement = conn.prepareStatement(builder.toString())) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject((i + 1), params.get(i));
            }

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }







    private EmbeddingsDB() {
        initVectorExtension();
        initVectorDbTable();

    }

    long countEmbeddings() {
        return (Long) loadResults(EmbeddingsSQL.COUNT_EMBEDDINGS).get(0).get("test");
    }


    public static final Lazy<EmbeddingsDB> impl = Lazy.of(EmbeddingsDB::new);


}