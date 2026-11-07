DROP TABLE IF EXISTS movimiento_stock;
DROP TABLE IF EXISTS pago_simulado;
DROP TABLE IF EXISTS detalle_pedido;
DROP TABLE IF EXISTS pedido;
DROP TABLE IF EXISTS direccion_cliente;
DROP TABLE IF EXISTS perfil_estilo;
DROP TABLE IF EXISTS cliente;
DROP TABLE IF EXISTS variante;
DROP TABLE IF EXISTS producto_categoria;
DROP TABLE IF EXISTS producto;
DROP TABLE IF EXISTS categoria;

CREATE TABLE categoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE producto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500),
    precio DECIMAL(10, 2) NOT NULL CHECK (precio > 0),
    activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE producto_categoria (
    producto_id BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    PRIMARY KEY (producto_id, categoria_id),
    CONSTRAINT fk_producto_categoria_producto FOREIGN KEY (producto_id)
        REFERENCES producto(id) ON DELETE CASCADE,
    CONSTRAINT fk_producto_categoria_categoria FOREIGN KEY (categoria_id)
        REFERENCES categoria(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE variante (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    talla VARCHAR(10) NOT NULL,
    color VARCHAR(40) NOT NULL,
    sku VARCHAR(40) NOT NULL UNIQUE,
    stock_actual INT NOT NULL DEFAULT 0 CHECK (stock_actual >= 0),
    stock_minimo INT NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
    CONSTRAINT uq_variante_producto_talla_color UNIQUE (producto_id, talla, color),
    CONSTRAINT fk_variante_producto FOREIGN KEY (producto_id)
        REFERENCES producto(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE perfil_estilo (
    cliente_id BIGINT PRIMARY KEY,
    talla_preferida VARCHAR(10),
    color_preferido VARCHAR(40),
    CONSTRAINT fk_perfil_estilo_cliente FOREIGN KEY (cliente_id)
        REFERENCES cliente(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE direccion_cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    etiqueta VARCHAR(40) NOT NULL,
    calle VARCHAR(160) NOT NULL,
    ciudad VARCHAR(100) NOT NULL,
    codigo_postal VARCHAR(15) NOT NULL,
    CONSTRAINT uq_direccion_cliente_etiqueta UNIQUE (cliente_id, etiqueta),
    CONSTRAINT fk_direccion_cliente FOREIGN KEY (cliente_id)
        REFERENCES cliente(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pedido (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    fecha TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('CONFIRMADO', 'CANCELADO')),
    total DECIMAL(10, 2) NOT NULL CHECK (total >= 0),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id)
        REFERENCES cliente(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE detalle_pedido (
    pedido_id BIGINT NOT NULL,
    variante_id BIGINT NOT NULL,
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario DECIMAL(10, 2) NOT NULL CHECK (precio_unitario > 0),
    PRIMARY KEY (pedido_id, variante_id),
    CONSTRAINT fk_detalle_pedido FOREIGN KEY (pedido_id)
        REFERENCES pedido(id) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_variante FOREIGN KEY (variante_id)
        REFERENCES variante(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pago_simulado (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL UNIQUE,
    importe DECIMAL(10, 2) NOT NULL CHECK (importe >= 0),
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('APROBADO', 'ANULADO')),
    CONSTRAINT fk_pago_pedido FOREIGN KEY (pedido_id)
        REFERENCES pedido(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE movimiento_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    variante_id BIGINT NOT NULL,
    pedido_id BIGINT,
    tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('REPOSICION', 'VENTA', 'DEVOLUCION_CANCELACION')),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    fecha TIMESTAMP NOT NULL,
    CONSTRAINT chk_movimiento_pedido CHECK (
        (tipo = 'REPOSICION' AND pedido_id IS NULL) OR
        (tipo IN ('VENTA', 'DEVOLUCION_CANCELACION') AND pedido_id IS NOT NULL)
    ),
    CONSTRAINT fk_movimiento_variante FOREIGN KEY (variante_id)
        REFERENCES variante(id) ON DELETE RESTRICT,
    CONSTRAINT fk_movimiento_pedido FOREIGN KEY (pedido_id)
        REFERENCES pedido(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
