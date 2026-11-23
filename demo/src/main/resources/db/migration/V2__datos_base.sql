-- Datos de referencia que cualquier despliegue necesita (roles, categorias, cupones).
INSERT INTO rol (id_rol, nombre_rol, descripcion) VALUES
  (1, 'cliente',       'Cliente que compra en la tienda'),
  (2, 'administrador', 'Gestiona todo el sistema'),
  (3, 'repostero',     'Prepara los pedidos en cocina'),
  (4, 'domiciliario',  'Realiza las entregas a domicilio'),
  (5, 'contador',      'Gestiona finanzas y facturas');

INSERT INTO categoria (id_categoria, nombre) VALUES
  (1, 'Tortas'), (2, 'Cheesecakes'), (3, 'Brownies'), (4, 'Macarons'),
  (5, 'Postres fríos'), (6, 'Cupcakes'), (7, 'Flanes'), (8, 'Galletas');

INSERT INTO cupon (codigo, descuento_pct, descripcion, activo) VALUES
  ('FLANBROSS10', 10, '10% en tu primera compra del mes', TRUE),
  ('BIENVENIDO',  15, '15% de bienvenida para clientes nuevos', TRUE),
  ('DULCE20',     20, '20% en pedidos superiores a $100.000', TRUE);
