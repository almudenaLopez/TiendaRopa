package es.tiendaderopa.orders;

import es.tiendaderopa.config.SimulationMode;
import es.tiendaderopa.orders.InventoryRepository.ArticleForOrder;
import es.tiendaderopa.orders.OrderRepository.PricedOrderLine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Coordina la creación completa de pedidos en una única transacción JDBC. */
public final class OrderService {
    private static final Logger LOGGER = LogManager.getLogger(OrderService.class);

    private final OrderRepository orderRepository = new OrderRepository();
    private final InventoryRepository inventoryRepository = new InventoryRepository();
    private final PaymentRepository paymentRepository = new PaymentRepository();

    public OrderSummary create(Connection connection, CreateOrderRequest request, SimulationMode simulationMode)
            throws SQLException, OrderException {
        boolean originalAutoCommit = connection.getAutoCommit();
        if (!originalAutoCommit) {
            throw new SQLException("No se puede iniciar el pedido dentro de otra transacción activa");
        }

        connection.setAutoCommit(false);
        try {
            orderRepository.requireExistingClient(connection, request.clientId());

            List<OrderLineRequest> sortedLines = request.lines().stream()
                    .sorted(Comparator.comparingLong(OrderLineRequest::articleId))
                    .toList();
            Map<Long, ArticleForOrder> lockedArticles = new HashMap<>();
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            int unitCount = 0;
            for (OrderLineRequest line : sortedLines) {
                ArticleForOrder article = inventoryRepository.lockArticleForOrder(connection, line.articleId());
                if (article.currentStock() < line.quantity()) {
                    throw new OrderException("Stock insuficiente para el artículo " + line.articleId()
                            + ": disponible " + article.currentStock() + ", solicitado " + line.quantity());
                }
                lockedArticles.put(line.articleId(), article);
                total = total.add(article.unitPrice().multiply(BigDecimal.valueOf(line.quantity())));
                unitCount += line.quantity();
            }
            total = total.setScale(2, RoundingMode.HALF_UP);

            long orderId = orderRepository.insertOrder(connection, request.clientId(), total);
            List<PricedOrderLine> pricedLines = sortedLines.stream()
                    .map(line -> new PricedOrderLine(line.articleId(), line.quantity(),
                            lockedArticles.get(line.articleId()).unitPrice()))
                    .toList();
            orderRepository.insertLines(connection, orderId, pricedLines);

            for (OrderLineRequest line : sortedLines) {
                inventoryRepository.sell(connection, orderId, lockedArticles.get(line.articleId()), line.quantity());
            }
            paymentRepository.insertApprovedPayment(connection, orderId, total);

            if (simulationMode == SimulationMode.FALLO_TRANSACCION) {
                throw new OrderException("Fallo de transacción simulado: se ha revertido el pedido");
            }

            connection.commit();
            LOGGER.info("Pedido {} creado: cliente={}, líneas={}, unidades={}, total={}",
                    orderId, request.clientId(), pricedLines.size(), unitCount, total);
            return new OrderSummary(orderId, total, pricedLines.size(), unitCount);
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
