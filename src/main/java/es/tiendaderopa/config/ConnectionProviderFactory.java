package es.tiendaderopa.config;

public final class ConnectionProviderFactory {
    private ConnectionProviderFactory() {
    }

    public static ConnectionProvider create(AppConfig config) {
        return switch (config.connectionMode()) {
            case DRIVER_MANAGER -> new DriverManagerConnectionProvider(config);
            case POOL -> new HikariConnectionProvider(config);
        };
    }
}
