package es.tiendaderopa.orders;

/** Error de validación o de negocio al crear un pedido. */
public final class OrderException extends Exception {
    public OrderException(String message) {
        super(message);
    }

    public OrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
