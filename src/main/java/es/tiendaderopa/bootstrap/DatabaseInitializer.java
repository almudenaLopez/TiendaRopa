package es.tiendaderopa.bootstrap;

import es.tiendaderopa.config.DatabaseEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {
    private static final Logger LOGGER = LogManager.getLogger(DatabaseInitializer.class);

    public void initialize(Connection connection, DatabaseEngine engine)
            throws SQLException, IOException {
        if (schemaAlreadyExists(connection)) {
            LOGGER.info("La base de datos ya contiene el esquema; se conserva tal como está");
            return;
        }

        String resource = switch (engine) {
            case H2 -> "/db/schema-h2.sql";
            case MYSQL -> "/db/schema-mysql.sql";
        };

        String script;
        try (InputStream input = DatabaseInitializer.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("No se encontró el esquema SQL: " + resource);
            }
            script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        int executedStatements = 0;
        try (Statement statement = connection.createStatement()) {
            for (String sql : script.split(";")) {
                String command = sql.trim();
                if (!command.isEmpty()) {
                    statement.execute(command);
                    executedStatements++;
                }
            }
        }
        LOGGER.info("Esquema {} preparado con {} sentencias", engine, executedStatements);
    }

    private static boolean schemaAlreadyExists(Connection connection) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(
                connection.getCatalog(), null, "CATEGORIA", new String[]{"TABLE"})) {
            if (tables.next()) {
                return true;
            }
        }
        try (ResultSet tables = connection.getMetaData().getTables(
                connection.getCatalog(), null, "categoria", new String[]{"TABLE"})) {
            return tables.next();
        }
    }
}
