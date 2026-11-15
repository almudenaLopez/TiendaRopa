package es.tiendaderopa.orders;

import java.util.List;

/** Datos necesarios para solicitar un pedido nuevo. */
public record CreateOrderRequest(long clientId, List<OrderLineRequest> lines) {
    public CreateOrderRequest {
        if (clientId <= 0) {
            throw new IllegalArgumentException("El identificador del cliente debe ser positivo");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos una línea");
        }
        lines = List.copyOf(lines);
        if (lines.stream().map(OrderLineRequest::articleId).distinct().count() != lines.size()) {
            throw new IllegalArgumentException("No se puede repetir un artículo en el mismo pedido");
        }
    }
}
