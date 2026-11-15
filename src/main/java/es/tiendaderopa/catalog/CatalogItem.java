package es.tiendaderopa.catalog;

import java.math.BigDecimal;

/** Una fila del catálogo: una prenda en una talla y color concretos. */
public record CatalogItem(
        long productId,
        String productName,
        String description,
        BigDecimal price,
        long articleId,
        String size,
        String color,
        String sku,
        int stock,
        int minimumStock) {
}
