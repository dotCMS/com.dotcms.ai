package com.dotcms.ai.db;


import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.pgvector.PGvector;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmbeddingsDB {


    public static final Lazy<EmbeddingsDB> impl = Lazy.of(EmbeddingsDB::new);


    private EmbeddingsDB() {
        initVectorExtension();
        initVectorDbTable();

    }

    public void initVectorExtension() {
        Logger.info(EmbeddingsDB.class, "Adding PGVector extension to database");
        runSQL(EmbeddingsSQL.INIT_VECTOR_EXTENSION);
    }

    public void initVectorDbTable() {
        Logger.info(EmbeddingsDB.class, "Adding table dot_embeddings to database");
        runSQL(EmbeddingsSQL.CREATE_EMBEDDINGS_TABLE);

        Logger.info(EmbeddingsDB.class, "Adding indexes to dot_embedding table");
        for (String index : EmbeddingsSQL.CREATE_TABLE_INDEXES) {
            runSQL(index);

        }


    }

    public void dropVectorDbTable() {
        Logger.info(EmbeddingsDB.class, "Dropping table dot_embeddings from database");
        runSQL(EmbeddingsSQL.DROP_EMBEDDINGS_TABLE);

    }

    public void runSQL(String sql) {
        try (Connection db = getPGVectorConnection()) {
            new DotConnect().setSQL(sql).loadResult(db);
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
            statement.setObject(++i, embeddings.indexName);
            statement.setObject(++i, embeddings.tokenCount);
            statement.setObject(++i, vector);
            statement.execute();

        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

    public List<EmbeddingsDTO> searchEmbeddings(EmbeddingsDTO dto) {


        StringBuilder sql = new StringBuilder(EmbeddingsSQL.SEARCH_EMBEDDINGS_SELECT_PREFIX.replace("{operator}", dto.operator));

        List<Object> params = appendParams(sql, dto);

        params.add(0, new PGvector(ArrayUtils.toPrimitive(dto.embeddings)));


        sql.append(" ) data where distance <= ? ");
        params.add(dto.threshold);

        sql.append(" order by distance limit ? offset ? ");
        params.add(dto.limit);
        params.add(dto.offset);


        try (Connection conn = getPGVectorConnection();
             PreparedStatement statement = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject((i + 1), params.get(i));
            }


            List<EmbeddingsDTO> results = new ArrayList<>();
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                float distance = rs.getFloat("distance");
                EmbeddingsDTO conEmbed = new EmbeddingsDTO.Builder()
                        .withContentType(rs.getString("content_type"))
                        .withField(rs.getString("field_var"))
                        .withIdentifier(rs.getString("identifier"))
                        .withInode(rs.getString("inode"))
                        .withTitle(rs.getString("title"))
                        .withLanguage(rs.getLong("language"))
                        .withIndexName(rs.getString("index_name"))
                        .withHost(rs.getString("host"))
                        .withTokenCount(rs.getInt("token_count"))
                        .withThreshold(distance)
                        .withExtractedText(rs.getString("extracted_text"))
                        .build();
                results.add(conEmbed);
            }
            return results;
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }

    }

    public int deleteEmbeddings(EmbeddingsDTO dto) {


        StringBuilder sql = new StringBuilder("delete from dot_embeddings where true ");
        List<Object> params = appendParams(sql, dto);
        Logger.info(EmbeddingsDB.class, "deleting embeddings:" + dto);

        try (Connection conn = getPGVectorConnection();
             PreparedStatement statement = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject((i + 1), params.get(i));
            }

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

    public List<Tuple2<String,Long>> countEmbeddings(EmbeddingsDTO dto) {
        StringBuilder sql = new StringBuilder(EmbeddingsSQL.COUNT_EMBEDDINGS_PREFIX);
        List<Object> params = appendParams(sql, dto);


        try (Connection conn = getPGVectorConnection();
             PreparedStatement statement = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject((i + 1), params.get(i));
            }
            List<Tuple2<String,Long>> results = new ArrayList<>();
            ResultSet rs = statement.executeQuery();
            while(rs.next()) {
                results.add(Tuple.of(rs.getString("index_name"), rs.getLong("test")));


            }
            return results;
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }


    }


    List<Object> appendParams(StringBuilder sql, EmbeddingsDTO dto){
        List<Object> params = new ArrayList<>();

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
        if (UtilMethods.isSet(dto.indexName)) {
            sql.append(" and index_name=? ");
            params.add(dto.indexName);
        }
        return params;



    }


}
