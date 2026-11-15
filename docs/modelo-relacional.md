# Modelo relacional: TiendaRopa

## Alcance

La aplicación simulará una tienda de ropa en Java con JDBC. Cada producto del catálogo puede ofrecer varios artículos, identificados por una combinación de talla y color; las existencias se controlan por artículo, no solo por producto. Los datos de demostración se cargan si la base de datos está vacía.

## Diagrama entidad-relación

```mermaid
erDiagram
    CATEGORIA ||--o{ PRODUCTO_CATEGORIA : clasifica
    PRODUCTO ||--o{ PRODUCTO_CATEGORIA : pertenece
    PRODUCTO ||--|{ ARTICULO : ofrece
    CLIENTE ||--|| PERFIL_ESTILO : tiene
    CLIENTE ||--o{ DIRECCION_CLIENTE : registra
    CLIENTE ||--o{ PEDIDO : realiza
    PEDIDO ||--|{ DETALLE_PEDIDO : contiene
    ARTICULO ||--o{ DETALLE_PEDIDO : se_vende
    PEDIDO ||--o| PAGO_SIMULADO : genera
    ARTICULO ||--o{ MOVIMIENTO_STOCK : registra
    PEDIDO o|--o{ MOVIMIENTO_STOCK : puede_originar

    CATEGORIA {
        int id PK
        varchar nombre UNIQUE
    }
    PRODUCTO {
        int id PK
        varchar nombre
        varchar descripcion
        decimal precio
        boolean activo
    }
    PRODUCTO_CATEGORIA {
        int producto_id PK,FK
        int categoria_id PK,FK
    }
    ARTICULO {
        int id PK
        int producto_id FK
        varchar talla
        varchar color
        varchar sku UNIQUE
        int stock_actual
        int stock_minimo
    }
    CLIENTE {
        int id PK
        varchar nombre
        varchar email UNIQUE
    }
    PERFIL_ESTILO {
        int cliente_id PK,FK
        varchar talla_preferida
        varchar color_preferido
    }
    DIRECCION_CLIENTE {
        int id PK
        int cliente_id FK
        varchar etiqueta
        varchar ciudad
    }
    PEDIDO {
        int id PK
        int cliente_id FK
        timestamp fecha
        varchar estado
        decimal total
    }
    DETALLE_PEDIDO {
        int pedido_id PK,FK
        int articulo_id PK,FK
        int cantidad
        decimal precio_unitario
    }
    PAGO_SIMULADO {
        int id PK
        int pedido_id FK,UNIQUE
        decimal importe
        varchar estado
    }
    MOVIMIENTO_STOCK {
        int id PK
        int articulo_id FK
        int pedido_id FK
        varchar tipo
        int cantidad
        timestamp fecha
    }
```

## Tablas y reglas de negocio

- `PRODUCTO` guarda la prenda y su precio actual. El precio histórico de una venta se conserva en `DETALLE_PEDIDO.precio_unitario`.
- `ARTICULO` representa una combinación concreta de producto, talla y color. `sku` es único y también se impide repetir `(producto_id, talla, color)`. El stock pertenece al artículo.
- `CATEGORIA` y `PRODUCTO` son muchos a muchos mediante `PRODUCTO_CATEGORIA`; una prenda puede estar en varias categorías.
- `CLIENTE` y `PERFIL_ESTILO` forman una relación 1:1. El perfil guarda preferencias sencillas que permiten una consulta propia de la tienda.
- Un cliente puede registrar varias `DIRECCION_CLIENTE` y realizar varios `PEDIDO`.
- `PEDIDO` y `ARTICULO` son muchos a muchos mediante `DETALLE_PEDIDO`. La clave compuesta `(pedido_id, articulo_id)` evita duplicar líneas del mismo artículo en un pedido.
- `PAGO_SIMULADO.pedido_id` es único, por lo que cada pedido tiene como máximo un pago ficticio.
- `MOVIMIENTO_STOCK` conserva entradas por reposición y salidas/devoluciones ligadas a pedidos. `cantidad` siempre es positiva; `tipo` determina si se suma o se resta.

## Relaciones que se usarán en las operaciones

El catálogo filtrará por categoría, talla, color y disponibilidad con `JOIN`. El detalle del cliente recuperará pedidos y sus prendas. Registrar un pedido coordinará pedido, líneas, stock, movimientos y pago en una sola transacción JDBC. Cancelarlo actualizará el estado, devolverá el stock y escribirá movimientos dentro de otra transacción; solo se permitirá si el pedido sigue confirmado. La reposición actualizará el artículo y registrará su movimiento atómicamente.

La consulta temática buscará prendas que coincidan con la talla y el color preferidos guardados en `PERFIL_ESTILO`. Las consultas de gestión mostrarán artículos con stock bajo y unidades vendidas por producto.

## Alcance inicial

Se usarán datos ficticios y pagos simulados. No se implementarán cesta web, autenticación, envíos ni pasarela de pago real. Las columnas monetarias serán `DECIMAL` en SQL y `BigDecimal` en Java.
