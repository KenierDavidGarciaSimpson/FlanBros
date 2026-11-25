# FlanBross — port del backend PHP a Spring Boot

Demo funcional del proyecto **FlanBross** (pastelería) con el backend reescrito en Java y el
frontend original reutilizado tal cual. Las páginas siguen siendo las mismas (Thymeleaf las
sirve) y el JavaScript habla con la API REST en vez de con los `*.php`.

## Requisitos y arranque

- **Docker Desktop** (PostgreSQL y, si quieres, el servidor en contenedor).
- **JDK 21** (lo fija `pom.xml`) y Maven Wrapper incluido, si corres el servidor fuera de Docker.
- Spring Boot **4.1.2**, base de datos **PostgreSQL 16**. Detalles en [`docs/base-de-datos.md`](docs/base-de-datos.md).

**Opción A — todo en contenedores** (la más simple para la sustentación):

```powershell
docker compose up -d --build      # PostgreSQL + servidor FlanBross en http://localhost:8080
docker compose logs -f app        # ver el arranque (Flyway crea las tablas y los datos demo)
docker compose down               # apagar (añade -v para borrar también la base de datos)
```

**Opción B — base en Docker, servidor desde Maven/IDE:**

```powershell
docker compose up -d db           # solo PostgreSQL en localhost:5432 (base flanbross, usuario flanbross)
.\mvnw.cmd spring-boot:run        # http://localhost:8080
.\mvnw.cmd test                   # contexto + seguridad (+ persistencia si hay PostgreSQL; ver `docs/pruebas.md`)
```

Sin base de datos (repositorios que leen `resources/datos/*.json`, los cambios se pierden al reiniciar):

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=memoria"
```

**Postman:** importa `postman/FlanBross.postman_collection.json` y `postman/FlanBross-Local.postman_environment.json`,
elige el entorno *FlanBross Local* y ejecuta la colección con *Runner* (41 peticiones con pruebas). Ver `docs/postman.md`.

**App Android:** ver `android/LEEME.md`. El emulador entra al servidor por `http://10.0.2.2:8080/`.

Cuentas de prueba: clientes `laura@demo.com` / `demo1234` · personal `maria@flanbross.co` (administradora) / `admin1234`.

Prueba de humo (~80 comprobaciones: inicia sesión con cada rol y revisa API, seguridad, vistas y estáticos; requiere la app y PostgreSQL corriendo):

```powershell
powershell -ExecutionPolicy Bypass -File .\pruebe-humo.ps1
# informe completo: %TEMP%\humo-flanbross.txt   (transcript en UTF-8)
```

## Capas

| Capa | Paquete | Qué contiene |
|------|---------|--------------|
| 1 Modelo | `Modelo`, `Modelo.dto` | Clases de dominio y DTOs (`RespuestaApi`, `NuevoPedido`, `ItemPedido`, `SesionUsuario`). Sin Spring. |
| Seguridad | `Seguridad` | Spring Security: sesión de servidor, roles por ruta, CSRF, límite de intentos. Ver `docs/seguridad.md`. |
| 2 Persistencia | `Persistencia` (interfaces), `Persistencia.jdbc` (PostgreSQL, por defecto), `Persistencia.memoria` (perfil `memoria`) | JDBC con SQL explícito y transacciones; el esquema lo versiona Flyway (`resources/db`). La implementación en memoria lee `resources/datos/*.json`. Los controladores no cambian. |
| 3 Controlador | `Controlador.api` (REST), `Controlador.vista` (páginas), `Controlador.util` | Endpoints `/api/**` y rutas de las vistas. |
| 4 Recursos | `resources/templates`, `resources/static`, `resources/datos` | Vistas, CSS/JS y datos de ejemplo. |

## API (sustituye a `backend/api/*.php`)

| Endpoint | Uso | Lo consume |
|----------|-----|-----------|
| `GET /api/productos` (`?id=`, `?categoria=`) | Catálogo | `catalogo.js`, `admin-productos.js` |
| `POST` · `PUT` · `DELETE /api/productos` (`/{id}` o id en el cuerpo) | CRUD del panel | `admin-productos.js` |
| `POST /api/productos/imagen` (multipart, campo `imagen`) | Subida de foto | `admin-productos.js` |
| `GET` · `PUT /api/inventario` | Existencias y estado | `admin-inventario.js` |
| `GET /api/pedidos` (`?id=`, `?id_cliente=`) · `POST` · `PUT` | Checkout e historial | `checkout.js`, `cuenta.js`, `admin-pedidos.js`, `domiciliario.js` |
| `POST /api/cupones` | Validación de cupón | `cupones.js` |
| `GET` · `PUT /api/clientes` | Listado y activar/desactivar | `admin-clientes.js` |
| `GET` · `POST` · `PUT` · `DELETE /api/empleados` | Personal | `admin-empleados.js` |
| `GET /api/facturas` | Facturación | `admin-facturas.js` |
| `GET` · `PUT /api/pagos` | Pagos | `admin-pagos.js` |
| `GET` (`?id_domiciliario=`) · `PUT /api/domicilios` (por `id_domicilio` o `id_pedido`) | Envíos | `admin-domicilios.js`, `domiciliario.js` |
| `GET /api/dashboard` | KPIs del panel | `admin-dashboard.js` |
| `POST /api/login` · `/api/registro` · `/api/recuperar` | Cuentas | `login.html`, `registro.html`, `recuperar.html` |
| `POST` (`GET` para pruebas) `/api/contacto` | Buzón | `contacto.html` |

Todas las respuestas usan la misma forma: `{ exito, mensaje, datos }`.

## Vistas

`PaginasControlador` sirve cada plantilla con su alias `.html` (`/catalogo.html`,
`/admin/dashboard.html`, `/pages/producto.html`, …) para que los enlaces del frontend original
sigan funcionando sin editarlos.

## Datos de ejemplo (`resources/datos`)

- Clientes: `laura@demo.com`, `juliana@demo.com`, `alex@demo.com` → clave `demo1234`.
- Personal: `maria@flanbross.co` (administrador), `luis@`, `vale@`, `felipe@`, `ana@` → clave `admin1234`.
- Cupones: `FLANBROSS10` (10 %), `BIENVENIDO` (15 %), `DULCE20` (20 % desde $100.000).
- Reglas de negocio conservadas: **IVA 8 %**, domicilio **$7.000**, número de factura
  `FAC-AAA-000N`, y "entregado" arrastra factura pagada + pago aprobado + domicilio entregado.

## Decisiones del port

1. **Rutas `/api/…` en vez de `*.php`**: se actualizaron las 20 referencias del frontend
   (`utils.js`, `admin/*.js`, `login.html`, `registro.html`, `recuperar.html`, `contacto.html`).
   El marcado de las plantillas no se tocó.
2. **Jackson 3** (`tools.jackson.*`): Spring Boot 4 ya no trae `com.fasterxml.jackson`, así que
   `LectorJson` usa el `ObjectMapper` de Spring y captura `JacksonException`.
3. **Repositorios en memoria** que leen JSON con `LectorJson` (UTF-8). Los cambios se pierden al
   reiniciar: es lo esperado mientras no haya base de datos.
4. **`/api/dashboard` responde las claves que lee `admin-dashboard.js`** (`ventas_mes`,
   `pedidos_mes`, `clientes_activos`, `domicilios_activos`, `ultimos_pedidos`, `stock_critico`).
   Se calculan por mes en curso, sobre facturas no anuladas.
5. **Imágenes subidas fuera del classpath** (`carpeta-imagenes/`, configurable con
   `flanbross.carpeta-imagenes`), publicadas en `/img/productos/**` por `ConfiguracionWeb`.
6. **`DELETE` acepta el id en el cuerpo** (`{"id_producto": N}`): es como lo manda el panel y
   `fetch` no permite adjuntar cuerpo en la variante de ruta.
7. **`PUT /api/domicilios` resuelve por `id_pedido`** cuando el repartidor todavía no conoce el
   id del domicilio.
8. **Fix en `carrito.html` y `checkout.html`**: `<main>` tenía dos atributos `class`. Thymeleaf
   3.1 no lo acepta y devolvía **500**; ahora es `class="contenido-pagina pagina-interior"`.
9. **`checkout.html` paso 4**: el desglose del cobro (`confirm-subtotal`, `confirm-descuento`,
   `confirm-domicilio`, `confirm-total`) no existía; `checkout.js` lo rellenaba con guardas, así
   que la pantalla de confirmación salía vacía sin lanzar error. Añadido el bloque.
10. **`admin/domiciliario.html`**: era una maqueta estática (tarjetas `entrega-009/010` con datos
    inventados y botones `onclick` a funciones que no existen en ningún JS). Se sustituyó por
    `<div id="lista-domicilios">` —que `renderizarDomicilios()` ya rellenaba— y se conectaron
    `nombre-domiciliario`, `avatar-domiciliario`, `stat-dom-pendientes` y `stat-dom-encamino`.

## Pendientes

- Recuperación de contraseña sin correo (modo demo inseguro) y otras limitaciones: ver [`docs/seguridad.md`](docs/seguridad.md).
- Las imágenes subidas no se validan por contenido (solo por extensión) ni se redimensionan.
