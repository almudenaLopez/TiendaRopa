package es.tiendaderopa.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DriverManagerConnectionProvider implements ConnectionProvider {
    private final AppConfig config;

    public DriverManagerConnectionProvider(AppConfig config) {
        this.config = config;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                config.jdbcUrl(), config.username(), config.password());
    }

    @Override
    public void close() {
        // DriverManager no mantiene un recurso de pool que deba cerrarse.
    }
}
