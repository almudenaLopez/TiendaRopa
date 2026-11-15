package es.tiendaderopa.catalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Recomendaciones según la talla y el color preferidos del cliente. */
public final class RecommendationRepository {

    public List<CatalogItem> recommendForClient(Connection connection, long clientId)
            throws SQLException, IllegalArgumentException {
        if (clientId <= 0) {
            throw new IllegalArgumentException("El identificador del cliente debe ser positivo");
        }
        String sql = "SELECT p.id AS product_id, p.nombre AS product_name, p.descripcion AS description, "
                + "p.precio AS price, a.id AS article_id, a.talla AS size, a.color AS color, "
                + "a.sku AS sku, a.stock_actual AS stock, a.stock_minimo AS minimum_stock "
                + "FROM cliente c JOIN perfil_estilo pe ON pe.cliente_id = c.id "
                + "JOIN articulo a ON LOWER(a.talla) = LOWER(pe.talla_preferida) "
                + "AND LOWER(a.color) = LOWER(pe.color_preferido) "
                + "JOIN producto p ON p.id = a.producto_id "
                + "WHERE c.id = ? AND p.activo = TRUE AND a.stock_actual > 0 "
                + "ORDER BY p.nombre, a.talla, a.color";
        List<CatalogItem> recommendations = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    recommendations.add(new CatalogItem(
                            results.getLong("product_id"), results.getString("product_name"),
                            results.getString("description"), results.getBigDecimal("price"),
                            results.getLong("article_id"), results.getString("size"),
                            results.getString("color"), results.getString("sku"),
                            results.getInt("stock"), results.getInt("minimum_stock")));
                }
            }
        }
        return recommendations;
    }
}
