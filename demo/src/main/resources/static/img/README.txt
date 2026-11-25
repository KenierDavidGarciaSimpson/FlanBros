=== IMÁGENES DE PRODUCTOS — FlanBross ===

Guarda aquí las fotos de los productos con los nombres exactos:

  img/productos/torta-tres-leches.jpg        → Torta Tres Leches
  img/productos/cheesecake-maracuya.jpg      → Cheesecake Maracuyá
  img/productos/brownie-con-nueces.jpg       → Brownie con Nueces
  img/productos/macarons-surtidos-x6.jpg     → Macarons Surtidos x6
  img/productos/creme-brulee.jpg             → Creme Brûlée
  img/productos/tiramisu-clasico.jpg         → Tiramisú Clásico
  img/productos/cupcake-red-velvet.jpg       → Cupcake Red Velvet
  img/productos/torta-selva-negra.jpg        → Torta Selva Negra
  img/productos/flan-de-coco.jpg             → Flan de Coco
  img/productos/galletas-de-mantequilla-x12.jpg → Galletas de Mantequilla x12

CÓMO AGREGAR TUS FOTOS:
  1. Copia tu imagen a esta carpeta (frontend/img/productos/)
  2. Cambia el nombre del archivo al que aparece en la tabla de arriba
  3. Si la imagen no está, el sistema muestra automáticamente un emoji de respaldo

FORMATOS ACEPTADOS: .jpg  .jpeg  .png  .webp
TAMAÑO RECOMENDADO: 600×600 px como mínimo, cuadradas si es posible

CAMBIAR RUTAS EN LA BASE DE DATOS:
  Después de subir las fotos, actualiza el campo 'imagen' en la tabla Producto:
  UPDATE Producto SET imagen = 'torta-tres-leches.jpg' WHERE id_producto = 1;
  UPDATE Producto SET imagen = 'cheesecake-maracuya.jpg' WHERE id_producto = 2;
  UPDATE Producto SET imagen = 'brownie-con-nueces.jpg' WHERE id_producto = 3;
  UPDATE Producto SET imagen = 'macarons-surtidos-x6.jpg' WHERE id_producto = 4;
  UPDATE Producto SET imagen = 'creme-brulee.jpg' WHERE id_producto = 5;
  UPDATE Producto SET imagen = 'tiramisu-clasico.jpg' WHERE id_producto = 6;
  UPDATE Producto SET imagen = 'cupcake-red-velvet.jpg' WHERE id_producto = 7;
  UPDATE Producto SET imagen = 'torta-selva-negra.jpg' WHERE id_producto = 8;
  UPDATE Producto SET imagen = 'flan-de-coco.jpg' WHERE id_producto = 9;
  UPDATE Producto SET imagen = 'galletas-de-mantequilla-x12.jpg' WHERE id_producto = 10;
