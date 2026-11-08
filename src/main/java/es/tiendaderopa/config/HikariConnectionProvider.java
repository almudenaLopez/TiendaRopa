package es.tiendaderopa.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class HikariConnectionProvider implements ConnectionProvider {
    private final HikariDataSource dataSource;

    public HikariConnectionProvider(AppConfig appConfig) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(appConfig.jdbcUrl());
        config.setUsername(appConfig.username());
        config.setPassword(appConfig.password());
        config.setMaximumPoolSize(appConfig.maximumPoolSize());
        config.setConnectionTimeout(appConfig.connectionTimeoutMillis());
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
