package es.tiendaderopa.orders;

import es.tiendaderopa.config.SimulationMode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Interacción por consola para crear un pedido. */
public final class OrderConsole {
    private final OrderService orderService = new OrderService();

    public void createOrder(Scanner scanner, Connection connection, SimulationMode simulationMode)
            throws SQLException {
        try {
            createOrderFlow(scanner, connection, simulationMode);
        } catch (IllegalArgumentException | OrderException exception) {
            System.out.println("No se pudo crear el pedido: " + exception.getMessage());
        }
    }

    private void createOrderFlow(Scanner scanner, Connection connection, SimulationMode simulationMode)
            throws SQLException, OrderException {
        showClients(connection);
        long clientId = readPositiveLong(scanner, "Identificador del cliente: ");
        showVariants(connection);

        List<OrderLineRequest> lines = new ArrayList<>();
        while (true) {
            long variantId = readNonNegativeLong(scanner,
                    "Identificador de variante (0 para terminar): ");
            if (variantId == 0) {
                break;
            }
            int quantity = readPositiveInt(scanner, "Cantidad: ");
            lines.add(new OrderLineRequest(variantId, quantity));
        }

        CreateOrderRequest request = new CreateOrderRequest(clientId, lines);
        OrderSummary summary = orderService.create(connection, request, simulationMode);
        System.out.printf("Pedido %d confirmado. Total: %s € (%d líneas, %d unidades).%n",
                summary.orderId(), summary.total(), summary.lineCount(), summary.unitCount());
    }

    private static void showClients(Connection connection) throws SQLException {
        String sql = "SELECT id, nombre, email FROM cliente ORDER BY id";
        System.out.println("\nClientes disponibles:");
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                System.out.printf("- %d | %s | %s%n",
                        results.getLong("id"), results.getString("nombre"), results.getString("email"));
            }
        }
    }

    private static void showVariants(Connection connection) throws SQLException {
        String sql = "SELECT v.id, p.nombre, v.talla, v.color, v.sku, p.precio, v.stock_actual "
                + "FROM variante v JOIN producto p ON p.id = v.producto_id "
                + "WHERE p.activo = TRUE ORDER BY p.nombre, v.talla, v.color";
        System.out.println("\nVariantes disponibles:");
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                System.out.printf("- variante %d | %s | talla %s | %s | %s | %s € | stock %d%n",
                        results.getLong("id"), results.getString("nombre"), results.getString("talla"),
                        results.getString("color"), results.getString("sku"),
                        results.getBigDecimal("precio"), results.getInt("stock_actual"));
            }
        }
    }

    private static long readPositiveLong(Scanner scanner, String prompt) {
        long value = readNonNegativeLong(scanner, prompt);
        if (value == 0) {
            throw new IllegalArgumentException("El identificador debe ser mayor que cero");
        }
        return value;
    }

    private static long readNonNegativeLong(Scanner scanner, String prompt) {
        System.out.print(prompt);
        try {
            long value = Long.parseLong(scanner.nextLine().trim());
            if (value < 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Escribe un número entero válido");
        }
    }

    private static int readPositiveInt(Scanner scanner, String prompt) {
        System.out.print(prompt);
        try {
            int value = Integer.parseInt(scanner.nextLine().trim());
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("La cantidad debe ser un entero mayor que cero");
        }
    }
}
