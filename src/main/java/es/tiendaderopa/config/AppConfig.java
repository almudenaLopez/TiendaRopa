package es.tiendaderopa.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public record AppConfig(
        DatabaseEngine databaseEngine,
        ConnectionMode connectionMode,
        SimulationMode simulationMode,
        String jdbcUrl,
        String username,
        String password,
        int maximumPoolSize,
        long connectionTimeoutMillis) {

    public static AppConfig load(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }

        DatabaseEngine engine = parseEnum(
                DatabaseEngine.class, required(properties, "database.engine"), "database.engine");
        ConnectionMode mode = parseEnum(
                ConnectionMode.class, required(properties, "connection.mode"), "connection.mode");
        SimulationMode simulationMode = parseEnum(
                SimulationMode.class, required(properties, "simulation.mode"), "simulation.mode");

        String prefix = engine.name().toLowerCase(Locale.ROOT);
        String url = required(properties, prefix + ".url");
        String username = properties.getProperty(prefix + ".user", "").trim();
        String password = properties.getProperty(prefix + ".password", "");
        int maximumPoolSize = positiveInt(properties, "pool.maximumPoolSize", 5);
        long connectionTimeoutMillis = positiveLong(properties, "pool.connectionTimeout", 30_000L);

        return new AppConfig(engine, mode, simulationMode, url, username, password,
                maximumPoolSize, connectionTimeoutMillis);
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta una propiedad obligatoria: " + key);
        }
        return value.trim();
    }

    private static int positiveInt(Properties properties, String key, int defaultValue) {
        int value = Integer.parseInt(properties.getProperty(key, Integer.toString(defaultValue)).trim());
        if (value <= 0) {
            throw new IllegalArgumentException(key + " debe ser mayor que cero");
        }
        return value;
    }

    private static long positiveLong(Properties properties, String key, long defaultValue) {
        long value = Long.parseLong(properties.getProperty(key, Long.toString(defaultValue)).trim());
        if (value <= 0) {
            throw new IllegalArgumentException(key + " debe ser mayor que cero");
        }
        return value;
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String value, String key) {
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Valor no válido para " + key + ": " + value, exception);
        }
    }
}
