package es.tiendaderopa.orders;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** JDBC de cabecera y líneas de pedido. */
public final class OrderRepository {

    public void requireExistingClient(Connection connection, long clientId)
            throws SQLException, OrderException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM cliente WHERE id = ?")) {
            statement.setLong(1, clientId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new OrderException("No existe el cliente con identificador " + clientId);
                }
            }
        }
    }

    public long insertOrder(Connection connection, long clientId, BigDecimal total) throws SQLException {
        String sql = "INSERT INTO pedido (cliente_id, fecha, estado, total) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, clientId);
            statement.setTimestamp(2, Timestamp.from(Instant.now()));
            statement.setString(3, "CONFIRMADO");
            statement.setBigDecimal(4, total);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("El gestor no devolvió el identificador del pedido");
    }

    public void insertLines(Connection connection, long orderId, List<PricedOrderLine> lines)
            throws SQLException {
        String sql = "INSERT INTO detalle_pedido (pedido_id, variante_id, cantidad, precio_unitario) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (PricedOrderLine line : lines) {
                statement.setLong(1, orderId);
                statement.setLong(2, line.variantId());
                statement.setInt(3, line.quantity());
                statement.setBigDecimal(4, line.unitPrice());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public record PricedOrderLine(long variantId, int quantity, BigDecimal unitPrice) {
    }
}
