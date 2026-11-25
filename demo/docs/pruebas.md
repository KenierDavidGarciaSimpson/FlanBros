# Pruebas de FlanBross

Hay tres niveles. Cada uno responde a una pregunta distinta.

| Nivel | Archivo | Qué comprueba | Necesita | Casos |
|---|---|---|---|---|
| Arranque | `DemoApplicationTests` | Que la aplicación levanta con todos sus componentes | Nada (perfil `memoria`) | 1 |
| Seguridad | `SeguridadApiTests` | Quién puede entrar a qué: roles, CSRF, redirecciones, login, bloqueo, aislamiento entre clientes | Nada (perfil `memoria`) | 14 |
| Persistencia | `RepositoriosJdbcTests` | Que los repositorios JDBC funcionan contra **PostgreSQL real**: transacciones, restricciones, BCrypt, tokens | PostgreSQL en `localhost:5432` | 11 |
| API con Postman | `postman/FlanBross.postman_collection.json` | La API con sesiones, CSRF y roles; 41 peticiones con aserciones | Aplicación + PostgreSQL | 41 |
| Sistema (extremo a extremo) | `pruebe-humo.ps1` | La API y las páginas con la aplicación corriendo, iniciando sesión con cada rol | Aplicación + PostgreSQL | ~80 comprobaciones |

## Cómo ejecutarlas

```powershell
docker compose up -d db   # PostgreSQL (necesario para Persistencia y el script de humo)
.\mvnw.cmd test           # Arranque + Seguridad + Persistencia
.\mvnw.cmd spring-boot:run
powershell -ExecutionPolicy Bypass -File .\pruebe-humo.ps1     # en otra consola
```

- `RepositoriosJdbcTests` usa una base aparte (`flanbross_test`, la crea `docker/init` la primera vez que se levanta el contenedor; si ya
  tenías el contenedor, `docker compose down -v` y volver a subirlo) con usuario `flanbross`/`flanbross`. Para otras credenciales: `DB_TEST_URL`, `DB_TEST_USUARIO`, `DB_TEST_CLAVE`.
  Antes de cada prueba se ejecuta `flyway.clean()` + `migrate()`, de modo que las pruebas **no dependen unas
  de otras**. Si no hay PostgreSQL, la clase se **omite** (no falla).
- El informe del script de humo queda en `%TEMP%\humo-flanbross.txt`. Se ejecuta con el perfil por defecto
  (PostgreSQL + datos demo), no con `memoria`: la comprobación de cuentas inactivas solo existe en la versión JDBC.

## Qué cubren las pruebas de persistencia

- **Transacción de pedido:** crear un pedido descuenta stock y genera factura (con IVA 8 %), pago pendiente y
  domicilio de $7.000. Si falla la segunda línea, **no queda nada a medias** (ni pedido, ni líneas, ni factura, ni
  stock descontado).
- **Cascada de estados:** `entregado` → factura pagada, pago aprobado, domicilio entregado con fecha;
  `cancelado` → domicilio fallido; un estado inventado se rechaza.
- **Inventario:** el descuento es atómico y nunca deja stock negativo.
- **Cuentas:** el login compara con BCrypt (ignora mayúsculas y espacios en el correo), bloquea cuentas inactivas;
  el registro guarda solo el hash; el token de recuperación se guarda cifrado (SHA-256) y sirve una sola vez.
- **Restricciones de la base:** estado inválido, cliente inexistente, stock negativo y usuario sin titular son
  rechazados por PostgreSQL aunque la aplicación no los valide.

## Qué NO está cubierto (declararlo en el informe)

- No hay pruebas automáticas del JavaScript del navegador (las páginas solo se verifican por el script de humo,
  que comprueba que cargan, no que se vean bien).
- No hay pruebas de carga ni de concurrencia real (dos compras simultáneas del mismo producto): el descuento
  atómico se prueba de forma secuencial.
- No se prueban los controladores uno a uno con datos inválidos (validación de entradas).
- Las pruebas de seguridad usan los repositorios en memoria; la parte de BCrypt y cuentas inactivas se prueba
  aparte en `RepositoriosJdbcTests`.
