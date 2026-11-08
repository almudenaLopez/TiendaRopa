package es.tiendaderopa.catalog;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/** Interfaz de consola sencilla para consultar el catálogo. */
public final class CatalogConsole {
    private final CatalogRepository catalogRepository = new CatalogRepository();

    public void run(Connection connection) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=== TiendaRopa ===");
            System.out.println("1. Buscar prendas en el catálogo");
            System.out.println("0. Salir");
            System.out.print("Opción: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> searchCatalog(scanner, connection);
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
        System.out.printf("Se encontraron %d variantes:%n", items.size());
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
