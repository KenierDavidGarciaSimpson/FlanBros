# Seguridad de FlanBross

Spring Security 7 (Spring Boot 4). Antes, la sesión vivía solo en `localStorage` y cualquiera podía
llamar a `/api/**` sin iniciar sesión (o cambiando el `id_cliente` de la petición). Ahora el
**servidor** decide.

## Cómo funciona

1. `POST /api/login` valida con BCrypt, renueva el id de sesión (contra fijación de sesión) y crea la
   sesión HTTP. El navegador recibe la cookie `JSESSIONID` (**HttpOnly**, **SameSite=Lax**, 8 h).
2. Cada ruta exige un rol (matriz de abajo). Lo que no está listado se **deniega**.
3. Sin sesión: la API responde **401** y las páginas (`/admin/**`, `/cuenta`) redirigen a `/login.html`.
   Con sesión pero sin permiso: **403**.
4. **CSRF:** el servidor entrega la cookie `XSRF-TOKEN`; `static/js/seguridad.js` (cargado en todas las
   páginas) envía su valor en la cabecera `X-XSRF-TOKEN` en cada POST/PUT/DELETE. Sin ella: 403.
5. `POST /api/logout` invalida la sesión del servidor.
6. **Propiedad de los datos:** un cliente solo ve *sus* pedidos y facturas, y un pedido nuevo se asigna
   al cliente de la sesión, no al `id_cliente` que mande el navegador. Un repartidor solo ve y modifica
   *sus* entregas y solo puede mover un pedido a `listo` o `entregado`.
7. **Fuerza bruta:** 5 intentos de login fallidos (misma IP y correo) en 15 min → **429** hasta que pase la ventana.
8. **Cuentas inactivas** (cliente o empleado) ya no pueden iniciar sesión.

## Matriz de acceso a la API

| Ruta | Público | Cliente | Repostero | Contador | Domiciliario | Administrador |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| `POST /api/login`, `/registro`, `/recuperar`, `/contacto`, `/cupones` | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `GET /api/productos` | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `POST/PUT/DELETE /api/productos`, `/api/inventario` | | | ✔ | | | ✔ |
| `/api/pagos` | | | | ✔ | | ✔ |
| `/api/facturas` | | ✔ (solo suyas) | | ✔ | | ✔ |
| `POST /api/pedidos` | | ✔ | | | | |
| `GET /api/pedidos` | | ✔ (solo suyos) | | | ✔ | ✔ |
| `PUT /api/pedidos` | | | | | ✔ (`listo`/`entregado`) | ✔ |
| `/api/domicilios` | | | | | ✔ (solo suyos) | ✔ |
| `/api/clientes`, `/empleados`, `/dashboard`, `GET /contacto` | | | | | | ✔ |

Páginas: `/admin/**` exige alguno de los cuatro roles del personal; `/cuenta` exige sesión; el resto del
sitio es público.

## Decisiones de diseño

| Decisión | Motivo |
|---|---|
| Sesión de servidor con cookie HttpOnly en vez de JWT en `localStorage` | JS no puede leer la cookie, así que un XSS no roba la sesión; se invalida al cerrar sesión. |
| CSRF activo con cookie + cabecera | La cookie de sesión viaja sola en peticiones de otros sitios; el token no. |
| Permisos por rol **y** por propiedad del dato | El rol no basta: un cliente autenticado no debe poder pedir los pedidos de otro (IDOR). |
| Denegar por defecto (`/api/**` → `denyAll`) | Un endpoint nuevo queda cerrado hasta que se le asigne rol. |
| `localStorage` solo para mostrar el nombre en la interfaz | La autoridad es la sesión del servidor; el navegador no decide permisos. |

## Pruebas

`SeguridadApiTests` (perfil `memoria`, sin MySQL) cubre: API privada sin sesión → 401, redirección al login,
matriz de roles, rechazo de escrituras sin CSRF, login correcto/incorrecto, bloqueo tras 5 fallos y
aislamiento entre clientes. Se ejecuta con `.\mvnw.cmd test`.

## Limitaciones conocidas (declararlas en el informe)

- **Recuperación de contraseña:** sin servidor de correo, en modo demo (`flanbross.recuperacion.mostrar-token=true`)
  el token se devuelve en la respuesta, y *cualquiera* podría cambiar la clave de cualquier cuenta.
  Para producción: `MOSTRAR_TOKEN_RECUPERACION=false` y enviar el token por correo (SMTP pendiente).
- Desactivar un usuario no cierra sus sesiones ya abiertas hasta que expiren (8 h).
- El limitador de intentos vive en memoria (no se comparte entre servidores; detrás de un proxy hay que
  configurar las cabeceras `X-Forwarded-For`).
- No hay política de Content-Security-Policy: las páginas usan *scripts* en línea.
- Las imágenes subidas se validan por extensión, no por contenido.
- En HTTPS poner `COOKIE_SEGURA=true`.
