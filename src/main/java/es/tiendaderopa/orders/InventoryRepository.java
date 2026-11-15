package es.tiendaderopa.orders;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/** Bloqueo de variantes, control de existencias y trazabilidad de las ventas. */
public final class InventoryRepository {

    public VariantForOrder lockVariantForOrder(Connection connection, long variantId)
            throws SQLException, OrderException {
        String sql = "SELECT v.stock_actual, p.precio "
                + "FROM variante v JOIN producto p ON p.id = v.producto_id "
                + "WHERE v.id = ? AND p.activo = TRUE FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, variantId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new OrderException("No existe una variante activa con identificador " + variantId);
                }
                return new VariantForOrder(variantId,
                        results.getInt("stock_actual"), results.getBigDecimal("precio"));
            }
        }
    }

    public void sell(Connection connection, long orderId, VariantForOrder variant, int quantity)
            throws SQLException, OrderException {
        String updateSql = "UPDATE variante SET stock_actual = stock_actual - ? "
                + "WHERE id = ? AND stock_actual >= ?";
        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
            statement.setInt(1, quantity);
            statement.setLong(2, variant.variantId());
            statement.setInt(3, quantity);
            if (statement.executeUpdate() != 1) {
                throw new OrderException("No hay existencias suficientes para la variante "
                        + variant.variantId());
            }
        }

        String movementSql = "INSERT INTO movimiento_stock "
                + "(variante_id, pedido_id, tipo, cantidad, fecha) VALUES (?, ?, 'VENTA', ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(movementSql)) {
            statement.setLong(1, variant.variantId());
            statement.setLong(2, orderId);
            statement.setInt(3, quantity);
            statement.setTimestamp(4, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }

    public record VariantForOrder(long variantId, int currentStock, BigDecimal unitPrice) {
    }
}
