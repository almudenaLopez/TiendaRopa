package es.tiendaderopa.catalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Consultas JDBC de solo lectura para el catálogo de prendas. */
public final class CatalogRepository {
    private static final String BASE_QUERY = """
            SELECT p.id AS product_id,
                   p.nombre AS product_name,
                   p.descripcion AS description,
                   p.precio AS price,
                   v.id AS variant_id,
                   v.talla AS size,
                   v.color AS color,
                   v.sku AS sku,
                   v.stock_actual AS stock,
                   v.stock_minimo AS minimum_stock
            FROM producto p
            JOIN variante v ON v.producto_id = p.id
            WHERE p.activo = TRUE
            """;

    public List<CatalogItem> search(Connection connection, CatalogFilter filter) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_QUERY);
        List<String> parameters = new ArrayList<>();

        if (hasText(filter.category())) {
            sql.append("""
                    AND EXISTS (
                        SELECT 1
                        FROM producto_categoria pc
                        JOIN categoria c ON c.id = pc.categoria_id
                        WHERE pc.producto_id = p.id
                          AND LOWER(c.nombre) = LOWER(?)
                    )
                    """);
            parameters.add(filter.category().trim());
        }
        if (hasText(filter.size())) {
            sql.append(" AND LOWER(v.talla) = LOWER(?)");
            parameters.add(filter.size().trim());
        }
        if (hasText(filter.color())) {
            sql.append(" AND LOWER(v.color) = LOWER(?)");
            parameters.add(filter.color().trim());
        }
        if (filter.onlyAvailable()) {
            sql.append(" AND v.stock_actual > 0");
        }
        sql.append(" ORDER BY p.nombre, v.talla, v.color");

        List<CatalogItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setString(i + 1, parameters.get(i));
            }
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    items.add(new CatalogItem(
                            results.getLong("product_id"),
                            results.getString("product_name"),
                            results.getString("description"),
                            results.getBigDecimal("price"),
                            results.getLong("variant_id"),
                            results.getString("size"),
                            results.getString("color"),
                            results.getString("sku"),
                            results.getInt("stock"),
                            results.getInt("minimum_stock")));
                }
            }
        }
        return items;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
