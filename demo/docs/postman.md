# Postman — pruebas de la API

## Importar
1. *Import* → selecciona `postman/FlanBross.postman_collection.json` y `postman/FlanBross-Local.postman_environment.json`.
2. Arriba a la derecha elige el entorno **FlanBross Local** (`baseUrl = http://localhost:8080`).
3. Con el servidor corriendo, abre la colección → **Run** (Collection Runner) → ejecuta todo **en orden**.

## Qué hay
| Carpeta | Qué demuestra |
|---|---|
| 0 · Preparación | El servidor responde 401 sin sesión y entrega la cookie `XSRF-TOKEN` |
| 1 · Autenticación | Login, sesión actual, clave incorrecta, registro, recuperación y logout |
| 2 · Catálogo | Endpoints públicos (sin sesión) |
| 3 · Flujo del cliente | Pedido completo (guarda `{{pedidoId}}`), IVA 8 %, rechazo por falta de stock, historial propio |
| 4 · Panel del administrador | Dashboard, inventario, pedidos, pagos, clientes, empleados, domicilios |
| 5 · Seguridad | Sin sesión → 401, sin token CSRF → 403, un cliente no puede ver pagos/empleados, una repartidora no puede cancelar |

Cada petición tiene pruebas (`pm.test`) que marcan verde/rojo en el Runner.

## Cómo funciona la sesión en Postman
- Postman guarda las cookies solo (`JSESSIONID`, `XSRF-TOKEN`), como un navegador.
- El **script de la colección** (pestaña *Pre-request*) lee la cookie `XSRF-TOKEN` y la manda en la cabecera
  `X-XSRF-TOKEN` en cada petición. Sin ese token, el servidor rechaza los POST/PUT/DELETE con 403.
- Para probar a mano una petición suelta, primero ejecuta `GET /api/sesion` (carpeta 0) y luego `POST /api/login`.

## Con la app móvil
La app usa exactamente los mismos endpoints y la misma sesión que esta colección. Si algo falla en la app,
reproduce la llamada en Postman: si funciona ahí, el problema está en la app; si no, en el servidor.
