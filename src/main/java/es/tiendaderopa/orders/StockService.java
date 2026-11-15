package es.tiendaderopa.orders;

import java.sql.Connection;
import java.sql.SQLException;

/** Reposición de existencias con movimiento de stock en una misma transacción. */
public final class StockService {
    private final StockRepository stockRepository = new StockRepository();

    public int replenish(Connection connection, long articleId, int quantity)
            throws SQLException, OrderException {
        if (articleId <= 0) {
            throw new IllegalArgumentException("El identificador del artículo debe ser positivo");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad que se repone debe ser mayor que cero");
        }
        boolean originalAutoCommit = connection.getAutoCommit();
        if (!originalAutoCommit) {
            throw new SQLException("No se puede reponer stock dentro de otra transacción activa");
        }
        connection.setAutoCommit(false);
        try {
            int newStock = stockRepository.replenish(connection, articleId, quantity);
            connection.commit();
            return newStock;
        } catch (SQLException | OrderException | RuntimeException exception) {
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
}
