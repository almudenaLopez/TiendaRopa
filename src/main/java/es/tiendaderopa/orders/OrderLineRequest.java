package es.tiendaderopa.orders;

/** Una variante y la cantidad que se quiere comprar. */
public record OrderLineRequest(long variantId, int quantity) {
    public OrderLineRequest {
        if (variantId <= 0) {
            throw new IllegalArgumentException("El identificador de variante debe ser positivo");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
    }
}
