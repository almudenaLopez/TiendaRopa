package es.tiendaderopa.orders;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Consultas necesarias para listar y cancelar pedidos confirmados. */
public final class OrderCancellationRepository {

    public List<OrderToCancel> findConfirmedOrders(Connection connection) throws SQLException {
        String sql = "SELECT p.id, c.nombre AS client_name, p.fecha, p.total "
                + "FROM pedido p JOIN cliente c ON c.id = p.cliente_id "
                + "WHERE p.estado = 'CONFIRMADO' ORDER BY p.id";
        List<OrderToCancel> orders = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                orders.add(new OrderToCancel(results.getLong("id"), results.getString("client_name"),
                        results.getTimestamp("fecha").toInstant(), results.getBigDecimal("total")));
            }
        }
        return orders;
    }

    public void lockConfirmedOrder(Connection connection, long orderId) throws SQLException, OrderException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM pedido WHERE id = ? AND estado = 'CONFIRMADO' FOR UPDATE")) {
            statement.setLong(1, orderId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new OrderException("El pedido no existe o ya está cancelado");
                }
            }
        }
    }

    public List<OrderLineToRestore> findOrderLines(Connection connection, long orderId) throws SQLException {
        String sql = "SELECT articulo_id, cantidad FROM detalle_pedido WHERE pedido_id = ? ORDER BY articulo_id";
        List<OrderLineToRestore> lines = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    lines.add(new OrderLineToRestore(results.getLong("articulo_id"),
                            results.getInt("cantidad")));
                }
            }
        }
        return lines;
    }

    public void markOrderCancelled(Connection connection, long orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE pedido SET estado = 'CANCELADO' WHERE id = ? AND estado = 'CONFIRMADO'")) {
            statement.setLong(1, orderId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("No se pudo cambiar el estado del pedido " + orderId);
            }
        }
    }

    public void markPaymentCancelled(Connection connection, long orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE pago_simulado SET estado = 'ANULADO' WHERE pedido_id = ? AND estado = 'APROBADO'")) {
            statement.setLong(1, orderId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("No se pudo anular el pago simulado del pedido " + orderId);
            }
        }
    }

    public record OrderToCancel(long orderId, String clientName,
                                java.time.Instant date, BigDecimal total) {
    }

    public record OrderLineToRestore(long articleId, int quantity) {
    }
}
