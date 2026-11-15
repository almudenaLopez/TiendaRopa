package es.tiendaderopa.orders;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/** Bloqueo de artículos, control de existencias y trazabilidad de las ventas. */
public final class InventoryRepository {

    public void restoreAfterCancellation(Connection connection, long orderId, long articleId, int quantity)
            throws SQLException {
        String updateSql = "UPDATE articulo SET stock_actual = stock_actual + ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
            statement.setInt(1, quantity);
            statement.setLong(2, articleId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("No existe el artículo " + articleId + " al cancelar el pedido");
            }
        }

        String movementSql = "INSERT INTO movimiento_stock "
                + "(articulo_id, pedido_id, tipo, cantidad, fecha) "
                + "VALUES (?, ?, 'DEVOLUCION_CANCELACION', ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(movementSql)) {
            statement.setLong(1, articleId);
            statement.setLong(2, orderId);
            statement.setInt(3, quantity);
            statement.setTimestamp(4, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }

    public ArticleForOrder lockArticleForOrder(Connection connection, long articleId)
            throws SQLException, OrderException {
        String sql = "SELECT a.stock_actual, p.precio "
                + "FROM articulo a JOIN producto p ON p.id = a.producto_id "
                + "WHERE a.id = ? AND p.activo = TRUE FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, articleId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new OrderException("No existe un artículo activo con identificador " + articleId);
                }
                return new ArticleForOrder(articleId,
                        results.getInt("stock_actual"), results.getBigDecimal("precio"));
            }
        }
    }

    public void sell(Connection connection, long orderId, ArticleForOrder article, int quantity)
            throws SQLException, OrderException {
        String updateSql = "UPDATE articulo SET stock_actual = stock_actual - ? "
                + "WHERE id = ? AND stock_actual >= ?";
        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
            statement.setInt(1, quantity);
            statement.setLong(2, article.articleId());
            statement.setInt(3, quantity);
            if (statement.executeUpdate() != 1) {
                throw new OrderException("No hay existencias suficientes para el artículo "
                        + article.articleId());
            }
        }

        String movementSql = "INSERT INTO movimiento_stock "
                + "(articulo_id, pedido_id, tipo, cantidad, fecha) VALUES (?, ?, 'VENTA', ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(movementSql)) {
            statement.setLong(1, article.articleId());
            statement.setLong(2, orderId);
            statement.setInt(3, quantity);
            statement.setTimestamp(4, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }

    public record ArticleForOrder(long articleId, int currentStock, BigDecimal unitPrice) {
    }
}
