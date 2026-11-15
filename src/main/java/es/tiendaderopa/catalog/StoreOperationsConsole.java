package es.tiendaderopa.catalog;

import es.tiendaderopa.orders.OrderCancellationRepository;
import es.tiendaderopa.orders.OrderCancellationService;
import es.tiendaderopa.orders.OrderException;
import es.tiendaderopa.orders.StockService;
import es.tiendaderopa.reports.ManagementRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/** Opciones de consola para pedidos, stock, informes y recomendaciones. */
public final class StoreOperationsConsole {
    private final OrderCancellationRepository cancellationRepository = new OrderCancellationRepository();
    private final OrderCancellationService cancellationService = new OrderCancellationService();
    private final StockService stockService = new StockService();
    private final ManagementRepository managementRepository = new ManagementRepository();
    private final RecommendationRepository recommendationRepository = new RecommendationRepository();

    public void cancelOrder(Scanner scanner, Connection connection) throws SQLException {
        try {
            List<OrderCancellationRepository.OrderToCancel> orders =
                    cancellationRepository.findConfirmedOrders(connection);
            if (orders.isEmpty()) {
                System.out.println("No hay pedidos confirmados que se puedan cancelar.");
                return;
            }
            System.out.println("\nPedidos confirmados:");
            for (OrderCancellationRepository.OrderToCancel order : orders) {
                System.out.printf("- pedido %d | %s | %s | %s €%n", order.orderId(), order.clientName(),
                        order.date(), order.total());
            }
            long orderId = readPositiveLong(scanner, "Identificador del pedido que quieres cancelar: ");
            cancellationService.cancel(connection, orderId);
            System.out.println("Pedido cancelado; el pago simulado se anuló y el stock se devolvió.");
        } catch (IllegalArgumentException | OrderException exception) {
            System.out.println("No se pudo cancelar el pedido: " + exception.getMessage());
        }
    }

    public void replenishStock(Scanner scanner, Connection connection) throws SQLException {
        try {
            showArticles(connection);
            long articleId = readPositiveLong(scanner, "Identificador del artículo: ");
            int quantity = readPositiveInt(scanner, "Unidades que quieres reponer: ");
            int newStock = stockService.replenish(connection, articleId, quantity);
            System.out.printf("Reposición guardada. Nuevo stock del artículo %d: %d unidades.%n",
                    articleId, newStock);
        } catch (IllegalArgumentException | OrderException exception) {
            System.out.println("No se pudo reponer stock: " + exception.getMessage());
        }
    }

    public void showLowStock(Connection connection) throws SQLException {
        List<ManagementRepository.LowStockArticle> articles =
                managementRepository.findLowStockArticles(connection);
        if (articles.isEmpty()) {
            System.out.println("No hay artículos con stock bajo.");
            return;
        }
        System.out.println("\nArtículos en el umbral mínimo o por debajo:");
        for (ManagementRepository.LowStockArticle article : articles) {
            System.out.printf("- %s | talla %s | %s | SKU %s | stock %d | mínimo %d%n",
                    article.productName(), article.size(), article.color(), article.sku(),
                    article.stock(), article.minimumStock());
        }
    }

    public void showSales(Connection connection) throws SQLException {
        System.out.println("\nUnidades vendidas por producto (pedidos confirmados):");
        for (ManagementRepository.ProductSales sales :
                managementRepository.findUnitsSoldByProduct(connection)) {
            System.out.printf("- %s: %d unidades%n", sales.productName(), sales.unitsSold());
        }
    }

    public void showRecommendations(Scanner scanner, Connection connection) throws SQLException {
        showClients(connection);
        try {
            long clientId = readPositiveLong(scanner, "Identificador del cliente: ");
            List<CatalogItem> items = recommendationRepository.recommendForClient(connection, clientId);
            if (items.isEmpty()) {
                System.out.println("No hay recomendaciones disponibles para ese cliente.");
                return;
            }
            System.out.println("\nArtículos recomendados según talla y color preferidos:");
            for (CatalogItem item : items) {
                System.out.printf("- %s | talla %s | %s | %s € | stock %d%n",
                        item.productName(), item.size(), item.color(), item.price(), item.stock());
            }
        } catch (IllegalArgumentException exception) {
            System.out.println("No se pudieron obtener recomendaciones: " + exception.getMessage());
        }
    }

    public void showCustomerOrders(Scanner scanner, Connection connection) throws SQLException {
        showClients(connection);
        try {
            long clientId = readPositiveLong(scanner, "Identificador del cliente: ");
            List<ManagementRepository.CustomerOrderLine> lines =
                    managementRepository.findOrdersByClient(connection, clientId);
            if (lines.isEmpty()) {
                System.out.println("No hay pedidos registrados para ese cliente.");
                return;
            }
            System.out.println("\nHistorial de pedidos del cliente:");
            long previousOrderId = -1;
            for (ManagementRepository.CustomerOrderLine line : lines) {
                if (line.orderId() != previousOrderId) {
                    System.out.printf("Pedido %d | %s | %s | total %s €%n",
                            line.orderId(), line.date(), line.status(), line.orderTotal());
                    previousOrderId = line.orderId();
                }
                System.out.printf("  - %s | talla %s | %s | %d uds. x %s €%n",
                        line.productName(), line.size(), line.color(), line.quantity(), line.unitPrice());
            }
        } catch (IllegalArgumentException exception) {
            System.out.println("No se pudo consultar el historial: " + exception.getMessage());
        }
    }

    private static void showArticles(Connection connection) throws SQLException {
        String sql = "SELECT a.id, p.nombre, a.talla, a.color, a.sku, a.stock_actual "
                + "FROM articulo a JOIN producto p ON p.id = a.producto_id "
                + "WHERE p.activo = TRUE ORDER BY p.nombre, a.talla, a.color";
        System.out.println("\nArtículos del catálogo:");
        try (var statement = connection.prepareStatement(sql);
             var results = statement.executeQuery()) {
            while (results.next()) {
                System.out.printf("- artículo %d | %s | talla %s | %s | SKU %s | stock %d%n",
                        results.getLong("id"), results.getString("nombre"), results.getString("talla"),
                        results.getString("color"), results.getString("sku"), results.getInt("stock_actual"));
            }
        }
    }

    private static void showClients(Connection connection) throws SQLException {
        String sql = "SELECT id, nombre, email FROM cliente ORDER BY id";
        System.out.println("\nClientes:");
        try (var statement = connection.prepareStatement(sql);
             var results = statement.executeQuery()) {
            while (results.next()) {
                System.out.printf("- %d | %s | %s%n", results.getLong("id"),
                        results.getString("nombre"), results.getString("email"));
            }
        }
    }

    private static long readPositiveLong(Scanner scanner, String prompt) {
        System.out.print(prompt);
        try {
            long value = Long.parseLong(scanner.nextLine().trim());
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Escribe un identificador entero mayor que cero");
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
            throw new IllegalArgumentException("Escribe una cantidad entera mayor que cero");
        }
    }
}
