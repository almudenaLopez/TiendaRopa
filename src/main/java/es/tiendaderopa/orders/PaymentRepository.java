package es.tiendaderopa.orders;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Registro del pago ficticio asociado a un pedido. */
public final class PaymentRepository {
    public void insertApprovedPayment(Connection connection, long orderId, BigDecimal amount)
            throws SQLException {
        String sql = "INSERT INTO pago_simulado (pedido_id, importe, estado) VALUES (?, ?, 'APROBADO')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setBigDecimal(2, amount);
            statement.executeUpdate();
        }
    }
}
