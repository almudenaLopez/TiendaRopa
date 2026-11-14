package es.tiendaderopa.orders;

import java.math.BigDecimal;

/** Resumen del pedido creado correctamente. */
public record OrderSummary(long orderId, BigDecimal total, int lineCount, int unitCount) {
}
