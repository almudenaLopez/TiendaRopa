package es.tiendaderopa.catalog;

import es.tiendaderopa.config.SimulationMode;
import es.tiendaderopa.orders.OrderConsole;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/** Menú principal de la tienda. */
public final class CatalogConsole {
    private final CatalogRepository catalogRepository = new CatalogRepository();
    private final OrderConsole orderConsole = new OrderConsole();
    private final StoreOperationsConsole operationsConsole = new StoreOperationsConsole();

    public void run(Connection connection, SimulationMode simulationMode) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=== TiendaRopa ===");
            System.out.println("1. Buscar prendas en el catálogo");
            System.out.println("2. Crear un pedido");
            System.out.println("3. Cancelar un pedido");
            System.out.println("4. Reponer artículos");
            System.out.println("5. Consultar artículos con stock bajo");
            System.out.println("6. Consultar unidades vendidas por producto");
            System.out.println("7. Recomendar artículos a un cliente");
            System.out.println("8. Consultar pedidos de un cliente");
            System.out.println("0. Salir");
            System.out.print("Opción: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> searchCatalog(scanner, connection);
                case "2" -> orderConsole.createOrder(scanner, connection, simulationMode);
                case "3" -> operationsConsole.cancelOrder(scanner, connection);
                case "4" -> operationsConsole.replenishStock(scanner, connection);
                case "5" -> operationsConsole.showLowStock(connection);
                case "6" -> operationsConsole.showSales(connection);
                case "7" -> operationsConsole.showRecommendations(scanner, connection);
                case "8" -> operationsConsole.showCustomerOrders(scanner, connection);
                case "0" -> running = false;
                default -> System.out.println("Opción no válida.");
            }
        }
    }

    private void searchCatalog(Scanner scanner, Connection connection) throws SQLException {
        System.out.println("Deja un filtro vacío para no aplicarlo.");
        String category = ask(scanner, "Categoría (por ejemplo, Camisetas): ");
        String size = ask(scanner, "Talla (por ejemplo, M): ");
        String color = ask(scanner, "Color (por ejemplo, Negro): ");
        String availableAnswer = ask(scanner, "¿Solo con stock? (s/N): ");

        CatalogFilter filter = new CatalogFilter(
                emptyAsNull(category), emptyAsNull(size), emptyAsNull(color),
                availableAnswer.equalsIgnoreCase("s") || availableAnswer.equalsIgnoreCase("si"));
        List<CatalogItem> items = catalogRepository.search(connection, filter);

        if (items.isEmpty()) {
            System.out.println("No se encontraron prendas con esos filtros.");
            return;
        }
        System.out.printf("Se encontraron %d artículos:%n", items.size());
        for (CatalogItem item : items) {
            System.out.printf("- %s | talla %s | %s | %s | %s € | stock: %d (mínimo %d)%n",
                    item.productName(), item.size(), item.color(), item.sku(), item.price(),
                    item.stock(), item.minimumStock());
        }
    }

    private static String ask(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static String emptyAsNull(String value) {
        return value.isBlank() ? null : value;
    }
}
