-- =====================================================================
-- FlanBross - Esquema relacional (MySQL 8 / MariaDB 10.6+)
-- Migracion Flyway V1: tablas, claves, restricciones e indices.
-- Dinero en pesos colombianos: DECIMAL(12,0) (el COP no usa centavos).
-- Estados validados con CHECK (MySQL aplica CHECK desde 8.0.16).
-- =====================================================================

-- ---------- Personas y acceso ----------

CREATE TABLE rol (
    id_rol       INT          NOT NULL AUTO_INCREMENT,
    nombre_rol   VARCHAR(30)  NOT NULL,
    descripcion  VARCHAR(120) NULL,
    PRIMARY KEY (id_rol),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cliente (
    id_cliente      INT          NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(80)  NOT NULL,
    apellido        VARCHAR(80)  NOT NULL,
    correo          VARCHAR(120) NOT NULL,
    telefono        VARCHAR(30)  NULL,
    direccion       VARCHAR(200) NULL,
    fecha_registro  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado_cliente  VARCHAR(10)  NOT NULL DEFAULT 'activo',
    PRIMARY KEY (id_cliente),
    CONSTRAINT uq_cliente_correo UNIQUE (correo),
    CONSTRAINT ck_cliente_estado CHECK (estado_cliente IN ('activo', 'inactivo'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE empleado (
    id_empleado        INT           NOT NULL AUTO_INCREMENT,
    nombre             VARCHAR(80)   NOT NULL,
    apellido           VARCHAR(80)   NOT NULL,
    documento          VARCHAR(20)   NULL,          -- opcional en el panel; unico cuando existe
    telefono           VARCHAR(30)   NULL,
    correo             VARCHAR(120)  NOT NULL,
    salario            DECIMAL(12,0) NOT NULL DEFAULT 0,
    fecha_contratacion DATE          NOT NULL,
    estado_empleado    VARCHAR(10)   NOT NULL DEFAULT 'activo',
    id_rol             INT           NOT NULL,
    PRIMARY KEY (id_empleado),
    CONSTRAINT uq_empleado_documento UNIQUE (documento),
    CONSTRAINT uq_empleado_correo    UNIQUE (correo),
    CONSTRAINT ck_empleado_estado    CHECK (estado_empleado IN ('activo', 'inactivo')),
    CONSTRAINT ck_empleado_salario   CHECK (salario >= 0),
    CONSTRAINT fk_empleado_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Una cuenta de acceso pertenece a UN cliente o a UN empleado (nunca a ambos).
-- La contrasena se guarda como hash BCrypt; el token de recuperacion, como SHA-256.
CREATE TABLE usuario (
    id_usuario        INT          NOT NULL AUTO_INCREMENT,
    id_rol            INT          NOT NULL,
    id_cliente        INT          NULL,
    id_empleado       INT          NULL,
    correo            VARCHAR(120) NOT NULL,
    contrasena_hash   VARCHAR(100) NOT NULL,
    token_reset_hash  CHAR(64)     NULL,
    token_expira      DATETIME     NULL,
    PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_correo   UNIQUE (correo),
    CONSTRAINT uq_usuario_cliente  UNIQUE (id_cliente),
    CONSTRAINT uq_usuario_empleado UNIQUE (id_empleado),
    CONSTRAINT ck_usuario_titular  CHECK (
        (id_cliente IS NOT NULL AND id_empleado IS NULL)
        OR (id_cliente IS NULL AND id_empleado IS NOT NULL)),
    CONSTRAINT fk_usuario_rol      FOREIGN KEY (id_rol)      REFERENCES rol (id_rol),
    CONSTRAINT fk_usuario_cliente  FOREIGN KEY (id_cliente)  REFERENCES cliente (id_cliente),
    CONSTRAINT fk_usuario_empleado FOREIGN KEY (id_empleado) REFERENCES empleado (id_empleado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_usuario_token ON usuario (token_reset_hash);

-- ---------- Catalogo e inventario ----------

CREATE TABLE categoria (
    id_categoria INT         NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(60) NOT NULL,
    PRIMARY KEY (id_categoria),
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE producto (
    id_producto       INT           NOT NULL AUTO_INCREMENT,
    nombre_producto   VARCHAR(100)  NOT NULL,
    descripcion       VARCHAR(500)  NULL,
    precio            DECIMAL(12,0) NOT NULL,
    id_categoria      INT           NOT NULL,
    fecha_vencimiento DATE          NULL,
    estado_producto   VARCHAR(12)   NOT NULL DEFAULT 'disponible',
    imagen            VARCHAR(200)  NULL,
    emoji             VARCHAR(16)   NULL,
    badge             VARCHAR(30)   NULL,
    PRIMARY KEY (id_producto),
    CONSTRAINT ck_producto_precio CHECK (precio >= 0),
    CONSTRAINT ck_producto_estado CHECK (estado_producto IN ('disponible', 'agotado', 'inactivo')),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria (id_categoria)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_producto_estado ON producto (estado_producto);

-- Fuente unica del stock (antes estaba duplicado en producto.stock).
-- estado_inventario (normal/bajo/critico) se calcula, no se guarda.
CREATE TABLE inventario (
    id_inventario       INT      NOT NULL AUTO_INCREMENT,
    id_producto         INT      NOT NULL,
    cantidad_disponible INT      NOT NULL DEFAULT 0,
    cantidad_minima     INT      NOT NULL DEFAULT 5,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_inventario),
    CONSTRAINT uq_inventario_producto UNIQUE (id_producto),
    CONSTRAINT ck_inventario_cantidad CHECK (cantidad_disponible >= 0),
    CONSTRAINT ck_inventario_minima   CHECK (cantidad_minima >= 0),
    CONSTRAINT fk_inventario_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cupon (
    codigo        VARCHAR(30)  NOT NULL,
    descuento_pct TINYINT      NOT NULL,
    descripcion   VARCHAR(200) NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (codigo),
    CONSTRAINT ck_cupon_pct CHECK (descuento_pct BETWEEN 1 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- Ventas ----------

CREATE TABLE pedido (
    id_pedido     INT           NOT NULL AUTO_INCREMENT,
    fecha_pedido  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado_pedido VARCHAR(15)   NOT NULL DEFAULT 'pendiente',
    total_pedido  DECIMAL(12,0) NOT NULL,
    observaciones VARCHAR(500)  NULL,
    id_cliente    INT           NOT NULL,
    PRIMARY KEY (id_pedido),
    CONSTRAINT ck_pedido_estado CHECK (estado_pedido IN
        ('pendiente', 'en_preparacion', 'listo', 'entregado', 'cancelado')),
    CONSTRAINT ck_pedido_total CHECK (total_pedido >= 0),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_pedido_cliente_fecha ON pedido (id_cliente, fecha_pedido);
CREATE INDEX idx_pedido_fecha         ON pedido (fecha_pedido);
CREATE INDEX idx_pedido_estado        ON pedido (estado_pedido);

CREATE TABLE detalle_pedido (
    id_detalle      INT           NOT NULL AUTO_INCREMENT,
    id_pedido       INT           NOT NULL,
    id_producto     INT           NOT NULL,
    cantidad        INT           NOT NULL,
    precio_unitario DECIMAL(12,0) NOT NULL,
    subtotal        DECIMAL(12,0) NOT NULL,
    PRIMARY KEY (id_detalle),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_subtotal CHECK (subtotal = precio_unitario * cantidad),
    CONSTRAINT fk_detalle_pedido   FOREIGN KEY (id_pedido)   REFERENCES pedido (id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_detalle_pedido ON detalle_pedido (id_pedido);

CREATE TABLE factura (
    id_factura     INT           NOT NULL AUTO_INCREMENT,
    numero_factura VARCHAR(20)   NOT NULL,
    fecha_factura  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal       DECIMAL(12,0) NOT NULL,
    impuestos      DECIMAL(12,0) NOT NULL,
    total_factura  DECIMAL(12,0) NOT NULL,
    estado_factura VARCHAR(10)   NOT NULL DEFAULT 'pendiente',
    id_cliente     INT           NOT NULL,
    id_pedido      INT           NOT NULL,
    PRIMARY KEY (id_factura),
    CONSTRAINT uq_factura_numero UNIQUE (numero_factura),
    CONSTRAINT uq_factura_pedido UNIQUE (id_pedido),
    CONSTRAINT ck_factura_estado CHECK (estado_factura IN ('pendiente', 'pagada', 'anulada')),
    CONSTRAINT ck_factura_total  CHECK (total_factura = subtotal + impuestos),
    CONSTRAINT fk_factura_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_factura_pedido  FOREIGN KEY (id_pedido)  REFERENCES pedido (id_pedido)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_factura_fecha ON factura (fecha_factura);

CREATE TABLE pago (
    id_pago         INT           NOT NULL AUTO_INCREMENT,
    fecha_pago      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    monto_pago      DECIMAL(12,0) NOT NULL,
    metodo_pago     VARCHAR(20)   NOT NULL,
    estado_pago     VARCHAR(10)   NOT NULL DEFAULT 'pendiente',
    referencia_pago VARCHAR(60)   NULL,
    id_factura      INT           NOT NULL,
    PRIMARY KEY (id_pago),
    CONSTRAINT ck_pago_metodo CHECK (metodo_pago IN
        ('contraentrega', 'tarjeta', 'transferencia', 'billetera_digital')),
    CONSTRAINT ck_pago_estado CHECK (estado_pago IN ('pendiente', 'aprobado', 'rechazado')),
    CONSTRAINT ck_pago_monto  CHECK (monto_pago >= 0),
    CONSTRAINT fk_pago_factura FOREIGN KEY (id_factura) REFERENCES factura (id_factura)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_pago_factura ON pago (id_factura);

CREATE TABLE domicilio (
    id_domicilio      INT           NOT NULL AUTO_INCREMENT,
    direccion_entrega VARCHAR(200)  NOT NULL,
    fecha_entrega     DATETIME      NULL,
    estado_entrega    VARCHAR(12)   NOT NULL DEFAULT 'pendiente',
    costo_domicilio   DECIMAL(12,0) NOT NULL DEFAULT 0,
    id_pedido         INT           NOT NULL,
    id_domiciliario   INT           NULL,
    PRIMARY KEY (id_domicilio),
    CONSTRAINT uq_domicilio_pedido UNIQUE (id_pedido),
    CONSTRAINT ck_domicilio_estado CHECK (estado_entrega IN
        ('pendiente', 'en_camino', 'entregado', 'fallido')),
    CONSTRAINT ck_domicilio_costo CHECK (costo_domicilio >= 0),
    CONSTRAINT fk_domicilio_pedido        FOREIGN KEY (id_pedido)       REFERENCES pedido (id_pedido),
    CONSTRAINT fk_domicilio_domiciliario  FOREIGN KEY (id_domiciliario) REFERENCES empleado (id_empleado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_domicilio_reparto ON domicilio (id_domiciliario, estado_entrega);

-- ---------- Contacto ----------

CREATE TABLE mensaje_contacto (
    id_mensaje  INT          NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(100) NOT NULL,
    correo      VARCHAR(120) NOT NULL,
    telefono    VARCHAR(30)  NULL,
    asunto      VARCHAR(80)  NOT NULL DEFAULT 'Otro motivo',
    mensaje     TEXT         NOT NULL,
    fecha_envio DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado      VARCHAR(10)  NOT NULL DEFAULT 'sin_leer',
    PRIMARY KEY (id_mensaje),
    CONSTRAINT ck_mensaje_estado CHECK (estado IN ('sin_leer', 'leido'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
