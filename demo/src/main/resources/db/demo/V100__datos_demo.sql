-- Datos de demostracion (clientes, personal, catalogo y pedidos de ejemplo).
-- Contrasenas de prueba: clientes = demo1234 · personal = admin1234 (guardadas como BCrypt).
-- NO incluir esta carpeta (db/demo) en un despliegue real.

INSERT INTO cliente (id_cliente, nombre, apellido, correo, telefono, direccion, fecha_registro, estado_cliente) VALUES
  (1, 'Laura', 'Vargas', 'laura@demo.com', '+57 300 111 2233', 'Bocagrande, Calle 5 #2-15', '2025-03-01 09:00:00', 'activo'),
  (2, 'Juliana', 'Mendoza', 'juliana@demo.com', '+57 315 444 5566', 'Av. Pedro Heredia #44-10', '2025-03-04 11:30:00', 'activo'),
  (3, 'Alejandro', 'Gómez', 'alex@demo.com', '+57 312 777 8899', 'Getsemaní, Calle 30 #9-80', '2025-03-10 16:45:00', 'activo'),
  (4, 'Isabella', 'Suárez', 'isa@demo.com', '+57 318 999 0011', 'El Centro, Calle 12 #3-22', '2025-03-18 10:15:00', 'activo'),
  (5, 'Carlos', 'Pérez', 'carlos@demo.com', '+57 321 222 3344', 'Manga, Transv. 54 #28-15', '2025-03-22 14:00:00', 'activo');

INSERT INTO empleado (id_empleado, nombre, apellido, documento, telefono, correo, salario, fecha_contratacion, estado_empleado, id_rol) VALUES
  (1, 'María', 'Bonilla', '1.045.221.001', '+57 300 000 0001', 'maria@flanbross.co', 2500000, '2023-01-15', 'activo', 2),
  (2, 'Luis', 'Martínez', '1.045.221.002', '+57 300 000 0002', 'luis@flanbross.co', 1800000, '2023-03-01', 'activo', 3),
  (3, 'Valentina', 'Florez', '1.045.221.003', '+57 300 000 0003', 'vale@flanbross.co', 1200000, '2024-01-10', 'activo', 4),
  (4, 'Felipe', 'Bernal', '1.045.221.004', '+57 300 000 0004', 'felipe@flanbross.co', 1200000, '2024-02-01', 'activo', 4),
  (5, 'Ana', 'Rondón', '1.045.221.005', '+57 300 000 0005', 'ana@flanbross.co', 2000000, '2023-06-15', 'activo', 5);

INSERT INTO usuario (id_usuario, id_rol, id_cliente, id_empleado, correo, contrasena_hash) VALUES
  (1, 1, 1, NULL, 'laura@demo.com', '$2b$10$l/NWuIl1OHyEJ.W/a.z5/.vgtyEiWN94XyP/tWmz6fnXCxWYo6lsO'),
  (2, 1, 2, NULL, 'juliana@demo.com', '$2b$10$l/NWuIl1OHyEJ.W/a.z5/.vgtyEiWN94XyP/tWmz6fnXCxWYo6lsO'),
  (3, 1, 3, NULL, 'alex@demo.com', '$2b$10$l/NWuIl1OHyEJ.W/a.z5/.vgtyEiWN94XyP/tWmz6fnXCxWYo6lsO'),
  (4, 2, NULL, 1, 'maria@flanbross.co', '$2b$10$sc4.qL2yPywHnhBihWN.De1oFHTUWPz5u4uyiN2afmVOTUtgmAG4a'),
  (5, 3, NULL, 2, 'luis@flanbross.co', '$2b$10$sc4.qL2yPywHnhBihWN.De1oFHTUWPz5u4uyiN2afmVOTUtgmAG4a'),
  (6, 4, NULL, 3, 'vale@flanbross.co', '$2b$10$sc4.qL2yPywHnhBihWN.De1oFHTUWPz5u4uyiN2afmVOTUtgmAG4a'),
  (7, 4, NULL, 4, 'felipe@flanbross.co', '$2b$10$sc4.qL2yPywHnhBihWN.De1oFHTUWPz5u4uyiN2afmVOTUtgmAG4a'),
  (8, 5, NULL, 5, 'ana@flanbross.co', '$2b$10$sc4.qL2yPywHnhBihWN.De1oFHTUWPz5u4uyiN2afmVOTUtgmAG4a');

INSERT INTO producto (id_producto, nombre_producto, descripcion, precio, id_categoria, fecha_vencimiento, estado_producto, imagen, emoji, badge) VALUES
  (1, 'Torta Tres Leches', 'Bizcocho esponjoso bañado en tres tipos de leche con crema chantilly.', 42000, 1, '2026-12-31', 'disponible', NULL, '🎂', 'Popular'),
  (2, 'Cheesecake Maracuyá', 'Tarta fría de queso crema con coulis de maracuyá sobre base de galleta.', 38000, 2, '2026-12-31', 'disponible', NULL, '🍰', 'Nuevo'),
  (3, 'Brownie con Nueces', 'Brownie denso de chocolate 70% con nueces tostadas, porción individual.', 8500, 3, '2026-12-31', 'disponible', NULL, '🍫', 'Popular'),
  (4, 'Macarons Surtidos x6', 'Caja de 6 macarons: vainilla, chocolate, limón, frutos rojos, pistacho y caramelo.', 24000, 4, '2026-12-31', 'disponible', NULL, '🍬', NULL),
  (5, 'Creme Brûlée', 'Crema francesa de vainilla con costra de azúcar caramelizada.', 14000, 5, '2026-12-31', 'disponible', NULL, '🍮', NULL),
  (6, 'Tiramisú Clásico', 'Capas de bizcochuelo con café, crema de mascarpone y cacao en polvo.', 16000, 5, '2026-12-31', 'disponible', NULL, '☕', 'Popular'),
  (7, 'Cupcake Red Velvet', 'Cupcake terciopelo rojo con frosting de queso crema, decorado a mano.', 7500, 6, '2026-12-31', 'disponible', NULL, '🧁', 'Nuevo'),
  (8, 'Torta Selva Negra', 'Capas de bizcocho de chocolate, crema chantilly y cerezas al marrasquino.', 45000, 1, '2026-12-31', 'disponible', NULL, '🍰', NULL),
  (9, 'Flan de Coco', 'Flan suave de leche de coco con caramelo artesanal, porción individual.', 11000, 7, '2026-12-31', 'disponible', NULL, '🍮', 'Popular'),
  (10, 'Galletas de Mantequilla x12', 'Caja de 12 galletas artesanales de mantequilla con azúcar decoradas.', 19000, 8, '2026-12-31', 'disponible', NULL, '🍪', NULL);

INSERT INTO inventario (id_inventario, id_producto, cantidad_disponible, cantidad_minima, fecha_actualizacion) VALUES
  (1, 1, 15, 5, '2025-04-29 08:00:00'),
  (2, 2, 12, 5, '2025-04-29 08:00:00'),
  (3, 3, 40, 10, '2025-04-29 08:00:00'),
  (4, 4, 25, 8, '2025-04-29 08:00:00'),
  (5, 5, 3, 8, '2025-04-29 08:00:00'),
  (6, 6, 18, 8, '2025-04-29 08:00:00'),
  (7, 7, 30, 10, '2025-04-29 08:00:00'),
  (8, 8, 8, 10, '2025-04-29 08:00:00'),
  (9, 9, 22, 8, '2025-04-29 08:00:00'),
  (10, 10, 35, 10, '2025-04-29 08:00:00');

INSERT INTO pedido (id_pedido, fecha_pedido, estado_pedido, total_pedido, observaciones, id_cliente) VALUES
  (1, '2026-11-01 10:30:00', 'entregado', 67500, 'Sin nueces en el brownie, por favor.', 1),
  (2, '2026-11-02 14:15:00', 'entregado', 53000, NULL, 1),
  (3, '2026-11-03 11:00:00', 'entregado', 85000, 'Cumpleaños, agregar velas.', 1),
  (4, '2026-11-05 16:30:00', 'entregado', 109000, NULL, 3),
  (5, '2026-11-07 09:45:00', 'en_preparacion', 55500, 'Entregar antes de las 12.', 2),
  (6, '2026-11-09 13:20:00', 'pendiente', 79000, NULL, 2);

INSERT INTO detalle_pedido (id_detalle, id_pedido, id_producto, cantidad, precio_unitario, subtotal) VALUES
  (1, 1, 1, 1, 42000, 42000),
  (2, 1, 3, 3, 8500, 25500),
  (3, 2, 2, 1, 38000, 38000),
  (4, 2, 7, 2, 7500, 15000),
  (5, 3, 8, 1, 45000, 45000),
  (6, 3, 4, 1, 24000, 24000),
  (7, 3, 6, 1, 16000, 16000),
  (8, 4, 1, 2, 42000, 84000),
  (9, 4, 5, 1, 14000, 14000),
  (10, 4, 9, 1, 11000, 11000),
  (11, 5, 7, 4, 7500, 30000),
  (12, 5, 3, 3, 8500, 25500),
  (13, 6, 2, 1, 38000, 38000),
  (14, 6, 10, 1, 19000, 19000),
  (15, 6, 9, 2, 11000, 22000);

INSERT INTO factura (id_factura, numero_factura, fecha_factura, subtotal, impuestos, total_factura, estado_factura, id_cliente, id_pedido) VALUES
  (1, 'FAC-2026-0001', '2026-11-01 10:30:00', 67500, 5400, 72900, 'pagada', 1, 1),
  (2, 'FAC-2026-0002', '2026-11-02 14:15:00', 53000, 4240, 57240, 'pagada', 1, 2),
  (3, 'FAC-2026-0003', '2026-11-03 11:00:00', 85000, 6800, 91800, 'pagada', 1, 3),
  (4, 'FAC-2026-0004', '2026-11-05 16:30:00', 109000, 8720, 117720, 'pagada', 3, 4),
  (5, 'FAC-2026-0005', '2026-11-07 09:45:00', 55500, 4440, 59940, 'pendiente', 2, 5),
  (6, 'FAC-2026-0006', '2026-11-09 13:20:00', 79000, 6320, 85320, 'pendiente', 2, 6);

INSERT INTO pago (id_pago, fecha_pago, monto_pago, metodo_pago, estado_pago, referencia_pago, id_factura) VALUES
  (1, '2026-11-01 10:35:00', 72900, 'contraentrega', 'aprobado', NULL, 1),
  (2, '2026-11-02 14:20:00', 57240, 'tarjeta', 'aprobado', 'TXN-0042', 2),
  (3, '2026-11-03 11:05:00', 91800, 'billetera_digital', 'aprobado', 'NQN-1187', 3),
  (4, '2026-11-05 16:35:00', 117720, 'transferencia', 'aprobado', 'TRF-5592', 4),
  (5, '2026-11-07 09:50:00', 59940, 'contraentrega', 'pendiente', NULL, 5),
  (6, '2026-11-09 13:25:00', 85320, 'tarjeta', 'pendiente', NULL, 6);

INSERT INTO domicilio (id_domicilio, direccion_entrega, fecha_entrega, estado_entrega, costo_domicilio, id_pedido, id_domiciliario) VALUES
  (1, 'Bocagrande, Calle 5 #2-15', '2026-11-01 11:20:00', 'entregado', 7000, 1, 3),
  (2, 'Bocagrande, Calle 5 #2-15', '2026-11-02 15:00:00', 'entregado', 7000, 2, 4),
  (3, 'Bocagrande, Calle 5 #2-15', '2026-11-03 12:10:00', 'entregado', 7000, 3, 3),
  (4, 'Getsemaní, Calle 30 #9-80', '2026-11-05 17:15:00', 'entregado', 7000, 4, 4),
  (5, 'Av. Pedro Heredia #44-10', NULL, 'en_camino', 7000, 5, 3),
  (6, 'Av. Pedro Heredia #44-10', NULL, 'pendiente', 7000, 6, NULL);
