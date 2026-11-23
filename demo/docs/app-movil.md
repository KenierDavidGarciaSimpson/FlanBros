# App móvil Android — contrato con el servidor

La interfaz que se presenta es una **app Android nativa (Kotlin + Jetpack Compose)** que consume la API REST del
servidor Spring Boot. El código está en `android/` (ver `android/LEEME.md`). Las páginas web (`templates/`) se
conservan como **panel de gestión** para el personal y como respaldo.

## Alcance

| Pieza | Dónde vive | Quién la usa |
|---|---|---|
| Tienda: catálogo, carrito, checkout, pedidos, cuenta, contacto | **App Android** | Cliente |
| Panel de gestión: productos, inventario, pedidos, pagos, clientes, empleados | Web (`/admin/dashboard`) | Administrador, repostero, contador |
| Entregas del repartidor | Web (`/admin/domiciliario`) — pendiente llevarlo a la app | Domiciliario |

La app rechaza el inicio de sesión de personal del negocio ("el personal usa el panel web").

## Cómo se autentica la app

Igual que el navegador, para no tocar la seguridad del servidor:

1. Al abrirse llama `GET /api/sesion`: **200** con el usuario si la cookie guardada sigue vigente, **401** si no.
   La respuesta (incluso el 401) trae la cookie `XSRF-TOKEN`.
2. `POST /api/login` crea la sesión; la cookie `JSESSIONID` (HttpOnly) se guarda en el teléfono.
3. En cada POST/PUT la app envía la cabecera `X-XSRF-TOKEN` con el valor de esa cookie (token CSRF).
4. Si el servidor responde 401 a una llamada protegida, la app da la sesión por cerrada.

*Alternativa considerada:* tokens JWT en cabecera `Authorization`. Se descartó por ahora porque obligaba a rediseñar
la seguridad ya probada; queda como trabajo futuro si se publica la app en tiendas.

## Endpoints que usa la app

Todas las respuestas tienen la forma `{ "exito": bool, "mensaje": texto, "datos": ... }` (en errores, `datos` es `[]`).

| Pantalla | Llamada | Cuerpo / parámetros | `datos` en caso de éxito |
|---|---|---|---|
| Arranque | `GET /api/sesion` | — | `{ usuario }` |
| Login | `POST /api/login` | `correo`, `contrasena` | `{ usuario }` · 401 clave mala · 429 bloqueado |
| Registro | `POST /api/registro` | `nombre`, `apellido`, `correo`, `contrasena` (mín. 8), `telefono?`, `direccion?` | `{ usuario }` (ya con sesión) |
| Recuperar | `POST /api/recuperar` | `correo` → luego `token`, `nueva_contrasena` | `{ token? }` (solo en modo demo) |
| Catálogo | `GET /api/productos` | — | lista de productos |
| Pedido | `POST /api/pedidos` | `direccion_entrega`, `observaciones?`, `metodo_pago`, `items[{id_producto, cantidad, precio_unitario}]` | `{ id_pedido, numero_factura, estado_pedido, subtotal, total }` |
| Mis pedidos | `GET /api/pedidos?id_cliente=N` | — (el servidor devuelve solo los del cliente de la sesión) | lista de pedidos con `detalles[]` |
| Contacto | `POST /api/contacto` | `nombre`, `correo`, `mensaje`, `asunto?`, `telefono?` | el mensaje guardado |
| Salir | `POST /api/logout` | — | 204 sin cuerpo |

`usuario` = `{ id_usuario, id_entidad, nombre, apellido, correo, nombre_rol, id_rol }`; para un cliente,
`id_entidad` es su `id_cliente`.

## Reglas que la app replica en pantalla

IVA 8 % sobre el subtotal (redondeo al peso, mitad hacia arriba) y total = subtotal + IVA. El precio que vale es
siempre el de la base de datos: la app envía `precio_unitario` pero el servidor lo ignora.

## Pendientes y decisiones abiertas

- **Cupones:** el servidor valida un cupón (`POST /api/cupones`) pero **no lo aplica al crear el pedido** ni guarda el
  descuento, y tampoco exige el mínimo de $100.000 de DULCE20. Por eso la app aún no ofrece cupones: primero hay que
  soportarlos en el servidor (columna de descuento en `pedido`, nueva migración, y aplicar el descuento antes del IVA).
- **Domicilio:** el costo ($7.000) se guarda aparte y **no se suma** a la factura. La app lo muestra como "se registra
  aparte". Confirmar si debe sumarse.
- **Repartidor en la app**, **fotos del catálogo** y **notificaciones de cambio de estado** quedan para las siguientes fases.
- **Producción:** la app debe hablar con un servidor **HTTPS** (el permiso de HTTP solo existe en *debug*), y el servidor
  con `COOKIE_SEGURA=true`.
