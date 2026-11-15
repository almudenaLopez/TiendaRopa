package es.tiendaderopa.orders;

import es.tiendaderopa.orders.OrderCancellationRepository.OrderLineToRestore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Cancela pedidos confirmados y devuelve el stock dentro de una transacción. */
public final class OrderCancellationService {
    private static final Logger LOGGER = LogManager.getLogger(OrderCancellationService.class);

    private final OrderCancellationRepository orderRepository = new OrderCancellationRepository();
    private final InventoryRepository inventoryRepository = new InventoryRepository();

    public void cancel(Connection connection, long orderId) throws SQLException, OrderException {
        if (orderId <= 0) {
            throw new IllegalArgumentException("El identificador del pedido debe ser positivo");
        }
        boolean originalAutoCommit = connection.getAutoCommit();
        if (!originalAutoCommit) {
            throw new SQLException("No se puede cancelar dentro de otra transacción activa");
        }
        connection.setAutoCommit(false);
        try {
            orderRepository.lockConfirmedOrder(connection, orderId);
            List<OrderLineToRestore> lines = orderRepository.findOrderLines(connection, orderId);
            if (lines.isEmpty()) {
                throw new OrderException("El pedido no tiene líneas y no se puede cancelar");
            }
            for (OrderLineToRestore line : lines) {
                inventoryRepository.restoreAfterCancellation(
                        connection, orderId, line.articleId(), line.quantity());
            }
            orderRepository.markPaymentCancelled(connection, orderId);
            orderRepository.markOrderCancelled(connection, orderId);
            connection.commit();
            LOGGER.info("Pedido {} cancelado y existencias devueltas", orderId);
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
