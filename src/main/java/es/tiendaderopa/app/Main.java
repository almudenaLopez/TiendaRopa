package es.tiendaderopa.app;

import es.tiendaderopa.catalog.CatalogConsole;
import es.tiendaderopa.bootstrap.DatabaseInitializer;
import es.tiendaderopa.bootstrap.InitialDataLoader;
import es.tiendaderopa.config.AppConfig;
import es.tiendaderopa.config.ConnectionProvider;
import es.tiendaderopa.config.ConnectionProviderFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

public final class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        try {
            Path configPath = Files.exists(Path.of("application.properties"))
                    ? Path.of("application.properties")
                    : Path.of("application.properties.example");
            AppConfig config = AppConfig.load(configPath);

            try (ConnectionProvider provider = ConnectionProviderFactory.create(config);
                 Connection connection = provider.getConnection()) {
                new DatabaseInitializer().initialize(connection, config.databaseEngine());
                new InitialDataLoader().load(connection);

                String databaseName = connection.getMetaData().getDatabaseProductName();
                LOGGER.info("Arranque completado: gestor={}, conexiones={}",
                        config.databaseEngine(), config.connectionMode());
                System.out.printf("Base de datos preparada: %s (%s, %s)%n",
                        databaseName, config.databaseEngine(), config.connectionMode());
                new CatalogConsole().run(connection);
            }
        } catch (Exception exception) {
            LOGGER.error("No se pudo iniciar la aplicación", exception);
            System.err.println("No se pudo iniciar la aplicación: " + exception.getMessage());
        }
    }
}
