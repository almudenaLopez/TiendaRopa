package es.tiendaderopa.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InitialDataLoader {
    private static final Logger LOGGER = LogManager.getLogger(InitialDataLoader.class);

    private static final List<String> CATEGORIES = List.of(
            "Básicos", "Camisetas", "Pantalones", "Sudaderas", "Abrigos");

    private static final List<ProductSeed> PRODUCTS = List.of(
            new ProductSeed("Camiseta básica", "Camiseta de algodón para uso diario", "12.99",
                    List.of("Camisetas", "Básicos")),
            new ProductSeed("Camiseta estampada", "Camiseta con estampado geométrico", "16.50",
                    List.of("Camisetas")),
            new ProductSeed("Pantalón vaquero recto", "Vaquero de corte recto", "39.90",
                    List.of("Pantalones")),
            new ProductSeed("Sudadera clásica", "Sudadera de algodón con capucha", "29.90",
                    List.of("Sudaderas", "Básicos")),
            new ProductSeed("Chaqueta vaquera", "Chaqueta vaquera de corte clásico", "49.90",
                    List.of("Abrigos")));

    private static final List<VariantSeed> VARIANTS = List.of(
            new VariantSeed("Camiseta básica", "S", "Negro", "CAM-BAS-NEG-S", 4, 5),
            new VariantSeed("Camiseta básica", "M", "Negro", "CAM-BAS-NEG-M", 12, 5),
            new VariantSeed("Camiseta básica", "M", "Blanco", "CAM-BAS-BLA-M", 8, 5),
            new VariantSeed("Camiseta básica", "L", "Blanco", "CAM-BAS-BLA-L", 3, 3),
            new VariantSeed("Camiseta estampada", "M", "Rojo", "CAM-EST-ROJ-M", 0, 3),
            new VariantSeed("Pantalón vaquero recto", "38", "Azul", "VAQ-REC-AZU-38", 7, 3),
            new VariantSeed("Pantalón vaquero recto", "40", "Azul", "VAQ-REC-AZU-40", 2, 3),
            new VariantSeed("Sudadera clásica", "M", "Gris", "SUD-CLA-GRI-M", 6, 2),
            new VariantSeed("Sudadera clásica", "L", "Gris", "SUD-CLA-GRI-L", 3, 2),
            new VariantSeed("Chaqueta vaquera", "M", "Azul", "CHA-VAQ-AZU-M", 5, 2));

    private static final List<ClientSeed> CLIENTS = List.of(
            new ClientSeed("Ana García", "ana.garcia@tiendaropa.test", "M", "Negro",
                    List.of(new AddressSeed("Casa", "Calle Mayor 12", "Madrid", "28013"),
                            new AddressSeed("Trabajo", "Avenida Central 25", "Madrid", "28020"))),
            new ClientSeed("Lucía Martín", "lucia.martin@tiendaropa.test", "S", "Blanco",
                    List.of(new AddressSeed("Casa", "Calle Luna 8", "Toledo", "45001"))),
            new ClientSeed("Pablo Santos", "pablo.santos@tiendaropa.test", "L", "Azul",
                    List.of(new AddressSeed("Casa", "Plaza Norte 4", "Segovia", "40001"))));

    public void load(Connection connection) throws SQLException {
        if (hasInitialData(connection)) {
            LOGGER.info("Ya hay datos en la tabla producto; se conserva la base de datos");
            return;
        }

        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            Map<String, Long> categoryIds = insertCategories(connection);
            Map<String, Long> productIds = insertProducts(connection);
            insertProductCategories(connection, categoryIds, productIds);
            List<SeededVariant> variants = insertVariants(connection, productIds);
            Map<String, Long> clientIds = insertClients(connection);
            insertClientProfiles(connection, clientIds);
            insertClientAddresses(connection, clientIds);
            insertOpeningStockMovements(connection, variants);
            connection.commit();
            LOGGER.info("Datos iniciales cargados: {} categorías, {} productos, {} variantes y {} clientes",
                    categoryIds.size(), productIds.size(), variants.size(), clientIds.size());
        } catch (SQLException | RuntimeException exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private static boolean hasInitialData(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM producto");
             ResultSet results = statement.executeQuery()) {
            return results.next() && results.getLong(1) > 0;
        }
    }

    private static Map<String, Long> insertCategories(Connection connection) throws SQLException {
        Map<String, Long> ids = new LinkedHashMap<>();
        String sql = "INSERT INTO categoria (nombre) VALUES (?)";
        for (String name : CATEGORIES) {
            ids.put(name, insertAndReturnId(connection, sql, statement -> statement.setString(1, name)));
        }
        return ids;
    }

    private static Map<String, Long> insertProducts(Connection connection) throws SQLException {
        Map<String, Long> ids = new LinkedHashMap<>();
        String sql = "INSERT INTO producto (nombre, descripcion, precio, activo) VALUES (?, ?, ?, ?)";
        for (ProductSeed product : PRODUCTS) {
            long id = insertAndReturnId(connection, sql, statement -> {
                statement.setString(1, product.name());
                statement.setString(2, product.description());
                statement.setBigDecimal(3, new BigDecimal(product.price()));
                statement.setBoolean(4, true);
            });
            ids.put(product.name(), id);
        }
        return ids;
    }

    private static void insertProductCategories(Connection connection, Map<String, Long> categoryIds,
                                                 Map<String, Long> productIds) throws SQLException {
        String sql = "INSERT INTO producto_categoria (producto_id, categoria_id) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (ProductSeed product : PRODUCTS) {
                for (String category : product.categories()) {
                    statement.setLong(1, productIds.get(product.name()));
                    statement.setLong(2, categoryIds.get(category));
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private static List<SeededVariant> insertVariants(Connection connection,
                                                      Map<String, Long> productIds) throws SQLException {
        List<SeededVariant> inserted = new ArrayList<>();
        String sql = "INSERT INTO variante "
                + "(producto_id, talla, color, sku, stock_actual, stock_minimo) VALUES (?, ?, ?, ?, ?, ?)";
        for (VariantSeed variant : VARIANTS) {
            long id = insertAndReturnId(connection, sql, statement -> {
                statement.setLong(1, productIds.get(variant.productName()));
                statement.setString(2, variant.size());
                statement.setString(3, variant.color());
                statement.setString(4, variant.sku());
                statement.setInt(5, variant.initialStock());
                statement.setInt(6, variant.minimumStock());
            });
            inserted.add(new SeededVariant(id, variant.initialStock()));
        }
        return inserted;
    }

    private static Map<String, Long> insertClients(Connection connection) throws SQLException {
        Map<String, Long> ids = new LinkedHashMap<>();
        String sql = "INSERT INTO cliente (nombre, email) VALUES (?, ?)";
        for (ClientSeed client : CLIENTS) {
            long id = insertAndReturnId(connection, sql, statement -> {
                statement.setString(1, client.name());
                statement.setString(2, client.email());
            });
            ids.put(client.email(), id);
        }
        return ids;
    }

    private static void insertClientProfiles(Connection connection, Map<String, Long> clientIds)
            throws SQLException {
        String sql = "INSERT INTO perfil_estilo (cliente_id, talla_preferida, color_preferido) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (ClientSeed client : CLIENTS) {
                statement.setLong(1, clientIds.get(client.email()));
                statement.setString(2, client.preferredSize());
                statement.setString(3, client.preferredColor());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void insertClientAddresses(Connection connection, Map<String, Long> clientIds)
            throws SQLException {
        String sql = "INSERT INTO direccion_cliente "
                + "(cliente_id, etiqueta, calle, ciudad, codigo_postal) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (ClientSeed client : CLIENTS) {
                for (AddressSeed address : client.addresses()) {
                    statement.setLong(1, clientIds.get(client.email()));
                    statement.setString(2, address.label());
                    statement.setString(3, address.street());
                    statement.setString(4, address.city());
                    statement.setString(5, address.postalCode());
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private static void insertOpeningStockMovements(Connection connection, List<SeededVariant> variants)
            throws SQLException {
        String sql = "INSERT INTO movimiento_stock (variante_id, pedido_id, tipo, cantidad, fecha) "
                + "VALUES (?, NULL, 'REPOSICION', ?, ?)";
        Timestamp now = Timestamp.from(Instant.now());
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (SeededVariant variant : variants) {
                if (variant.initialStock() == 0) {
                    continue;
                }
                statement.setLong(1, variant.id());
                statement.setInt(2, variant.initialStock());
                statement.setTimestamp(3, now);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static long insertAndReturnId(Connection connection, String sql, SqlBinder binder)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("El gestor no devolvió el identificador generado");
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private record ProductSeed(String name, String description, String price, List<String> categories) {
    }

    private record VariantSeed(String productName, String size, String color, String sku,
                               int initialStock, int minimumStock) {
    }

    private record SeededVariant(long id, int initialStock) {
    }

    private record ClientSeed(String name, String email, String preferredSize, String preferredColor,
                              List<AddressSeed> addresses) {
    }

    private record AddressSeed(String label, String street, String city, String postalCode) {
    }
}
