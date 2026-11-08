package es.tiendaderopa.catalog;

/**
 * Filtros opcionales para buscar prendas en el catálogo.
 * Un valor null o en blanco significa que no se aplica ese filtro.
 */
public record CatalogFilter(String category, String size, String color, boolean onlyAvailable) {

    public static CatalogFilter all() {
        return new CatalogFilter(null, null, null, false);
    }
}
