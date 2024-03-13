package com.dotcms.ai.db;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.vavr.Lazy;
import javax.sql.DataSource;


class PgVectorDataSource {


    public static final Lazy<DataSource> datasource = Lazy.of(() -> resolveDataSource());



    private static DataSource resolveDataSource() {
        if (UtilMethods.isEmpty(Config.getStringProperty("AI_DB_BASE_URL", null))) {
            return DbConnectionFactory.getDataSource();
        }
        return internalDatasource();


    }

    private static DataSource internalDatasource() {
        String userName = Config.getStringProperty("AI_DB_USERNAME");
        String password = Config.getStringProperty("AI_DB_PASSWORD");
        String dbUrl = Config.getStringProperty("AI_DB_BASE_URL");
        int maxConnections = Config.getIntProperty("AI_DB_MAX_TOTAL", 50);

        final HikariConfig config = new HikariConfig();
        config.setUsername(userName);
        config.setPassword(password);
        config.setJdbcUrl(dbUrl);
        config.setMaximumPoolSize(maxConnections);
        return new HikariDataSource(config);


    }


}
