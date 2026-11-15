package es.tiendaderopa.reports;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Consultas de gestión de stock y ventas. */
public final class ManagementRepository {

    public List<LowStockArticle> findLowStockArticles(Connection connection) throws SQLException {
        String sql = "SELECT a.id AS article_id, p.nombre AS product_name, a.talla AS size, "
                + "a.color AS color, a.sku AS sku, a.stock_actual AS stock, "
                + "a.stock_minimo AS minimum_stock "
                + "FROM articulo a JOIN producto p ON p.id = a.producto_id "
                + "WHERE p.activo = TRUE AND a.stock_actual <= a.stock_minimo "
                + "ORDER BY a.stock_actual, p.nombre, a.talla, a.color";
        List<LowStockArticle> articles = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                articles.add(new LowStockArticle(results.getLong("article_id"),
                        results.getString("product_name"), results.getString("size"),
                        results.getString("color"), results.getString("sku"),
                        results.getInt("stock"), results.getInt("minimum_stock")));
            }
        }
        return articles;
    }

    public List<ProductSales> findUnitsSoldByProduct(Connection connection) throws SQLException {
        String sql = "SELECT p.id AS product_id, p.nombre AS product_name, "
                + "COALESCE(SUM(CASE WHEN pe.estado = 'CONFIRMADO' THEN dp.cantidad ELSE 0 END), 0) "
                + "AS units_sold "
                + "FROM producto p "
                + "LEFT JOIN articulo a ON a.producto_id = p.id "
                + "LEFT JOIN detalle_pedido dp ON dp.articulo_id = a.id "
                + "LEFT JOIN pedido pe ON pe.id = dp.pedido_id "
                + "GROUP BY p.id, p.nombre ORDER BY units_sold DESC, p.nombre";
        List<ProductSales> sales = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                sales.add(new ProductSales(results.getLong("product_id"),
                        results.getString("product_name"), results.getLong("units_sold")));
            }
        }
        return sales;
    }

    public List<CustomerOrderLine> findOrdersByClient(Connection connection, long clientId)
            throws SQLException {
        String sql = "SELECT p.id AS order_id, p.fecha AS order_date, p.estado AS order_status, "
                + "p.total AS order_total, pr.nombre AS product_name, a.talla AS size, "
                + "a.color AS color, dp.cantidad AS quantity, dp.precio_unitario AS unit_price "
                + "FROM pedido p JOIN detalle_pedido dp ON dp.pedido_id = p.id "
                + "JOIN articulo a ON a.id = dp.articulo_id "
                + "JOIN producto pr ON pr.id = a.producto_id "
                + "WHERE p.cliente_id = ? ORDER BY p.fecha DESC, p.id, pr.nombre";
        List<CustomerOrderLine> lines = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    lines.add(new CustomerOrderLine(results.getLong("order_id"),
                            results.getTimestamp("order_date").toInstant(), results.getString("order_status"),
                            results.getBigDecimal("order_total"), results.getString("product_name"),
                            results.getString("size"), results.getString("color"),
                            results.getInt("quantity"), results.getBigDecimal("unit_price")));
                }
            }
        }
        return lines;
    }

    public record LowStockArticle(long articleId, String productName, String size,
                                  String color, String sku, int stock, int minimumStock) {
    }

    public record ProductSales(long productId, String productName, long unitsSold) {
    }

    public record CustomerOrderLine(long orderId, Instant date, String status, BigDecimal orderTotal,
                                    String productName, String size, String color,
                                    int quantity, BigDecimal unitPrice) {
    }
}
