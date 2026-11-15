package es.tiendaderopa.orders;

/** Un artículo y la cantidad que se quiere comprar. */
public record OrderLineRequest(long articleId, int quantity) {
    public OrderLineRequest {
        if (articleId <= 0) {
            throw new IllegalArgumentException("El identificador de artículo debe ser positivo");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
    }
}
