package es.tiendaderopa.catalog;

import java.math.BigDecimal;

/** Una fila del catálogo: una prenda junto con una de sus variantes. */
public record CatalogItem(
        long productId,
        String productName,
        String description,
        BigDecimal price,
        long variantId,
        String size,
        String color,
        String sku,
        int stock,
        int minimumStock) {
}
