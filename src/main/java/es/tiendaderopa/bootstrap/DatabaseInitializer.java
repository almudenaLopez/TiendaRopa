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
            migrateArticleNames(connection, engine);
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
        return tableExists(connection, "categoria");
    }

    private static void migrateArticleNames(Connection connection, DatabaseEngine engine)
            throws SQLException {
        boolean oldTableExists = tableExists(connection, "variante");
        boolean newTableExists = tableExists(connection, "articulo");
        if (oldTableExists && !newTableExists) {
            String renameTable = engine == DatabaseEngine.MYSQL
                    ? "RENAME TABLE variante TO articulo"
                    : "ALTER TABLE variante RENAME TO articulo";
            try (Statement statement = connection.createStatement()) {
                statement.execute(renameTable);
            }
        }

        renameColumnIfNeeded(connection, "detalle_pedido", "variante_id", "articulo_id");
        renameColumnIfNeeded(connection, "movimiento_stock", "variante_id", "articulo_id");
        if (oldTableExists || columnExists(connection, "detalle_pedido", "articulo_id")) {
            LOGGER.info("Nombre de la tabla y referencias migrados a artículo sin recrear los datos");
        }
    }

    private static void renameColumnIfNeeded(Connection connection, String table,
                                             String oldName, String newName) throws SQLException {
        if (!tableExists(connection, table)
                || !columnExists(connection, table, oldName)
                || columnExists(connection, table, newName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " RENAME COLUMN " + oldName + " TO " + newName);
        }
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        for (String candidate : new String[]{tableName.toUpperCase(), tableName.toLowerCase()}) {
            try (ResultSet tables = connection.getMetaData().getTables(
                    connection.getCatalog(), null, candidate, new String[]{"TABLE"})) {
                if (tables.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName)
            throws SQLException {
        for (String table : new String[]{tableName.toUpperCase(), tableName.toLowerCase()}) {
            for (String column : new String[]{columnName.toUpperCase(), columnName.toLowerCase()}) {
                try (ResultSet columns = connection.getMetaData().getColumns(
                        connection.getCatalog(), null, table, column)) {
                    if (columns.next()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
