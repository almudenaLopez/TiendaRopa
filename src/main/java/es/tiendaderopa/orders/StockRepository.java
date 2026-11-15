package es.tiendaderopa.orders;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/** JDBC para reponer artículos y registrar entradas de stock. */
public final class StockRepository {

    public int replenish(Connection connection, long articleId, int quantity) throws SQLException, OrderException {
        int currentStock;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT stock_actual FROM articulo WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, articleId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new OrderException("No existe el artículo " + articleId);
                }
                currentStock = results.getInt("stock_actual");
            }
        }
        if (quantity > Integer.MAX_VALUE - currentStock) {
            throw new OrderException("La reposición supera el máximo de stock permitido");
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE articulo SET stock_actual = stock_actual + ? WHERE id = ?")) {
            statement.setInt(1, quantity);
            statement.setLong(2, articleId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("No se pudo actualizar el stock del artículo " + articleId);
            }
        }

        String movementSql = "INSERT INTO movimiento_stock "
                + "(articulo_id, pedido_id, tipo, cantidad, fecha) VALUES (?, NULL, 'REPOSICION', ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(movementSql)) {
            statement.setLong(1, articleId);
            statement.setInt(2, quantity);
            statement.setTimestamp(3, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
        return currentStock + quantity;
    }
}
