# Plan de 2 días — FlanBross

**Regla de oro:** nada de esto se ha compilado todavía en sus equipos. Lo primero es que compile y corra;
las funciones nuevas (cupones, repartidor en la app, fotos) **solo si sobra tiempo**.

## Reparto

| Persona | Responsabilidad | Carpeta |
|---|---|---|
| **A · Servidor y datos** | Docker, PostgreSQL, que el servidor compile y pase sus pruebas | `src/`, `docker-compose.yml`, `Dockerfile` |
| **B · App Android** | Montar el proyecto, compilarlo y conectarlo al servidor | `android/` |
| **C · Postman, informe y sustentación** | Colección de Postman corriendo, documento, diagramas, guion | `postman/`, `docs/` |

## Día 1 — que todo corra

| Hora aprox. | A · Servidor | B · App | C · Postman / informe |
|---|---|---|---|
| 0–2 h | `docker compose up -d db` y `.\mvnw.cmd test`. **Pegar aquí los errores de compilación** y corregirlos | Crear proyecto Compose `com.flanbross.app`, pegar el código, sincronizar Gradle | Instalar Postman. Leer `docs/postman.md` y el documento técnico |
| 2–5 h | `.\mvnw.cmd spring-boot:run` → abrir `http://localhost:8080`; probar login con `laura@demo.com / demo1234` | Compilar la app; corregir errores uno por uno | Importar colección y entorno; correr **carpeta 0 y 1** a mano |
| 5–8 h | `docker compose up -d --build` (servidor también en contenedor) | Correr la app en el emulador contra `10.0.2.2:8080`; probar login y catálogo | Correr **toda la colección** en el Runner; anotar lo que falle |
| Cierre | **Subir a GitHub** lo que funcione (commit pequeño, mensaje claro) | Ídem | Capturas del Runner en verde |

**Punto de control del día 1:** el servidor responde, Postman pasa en verde y la app muestra el catálogo.
Si algo no llegó, es la prioridad de la mañana siguiente.

## Día 2 — completar y ensayar

| Bloque | A · Servidor | B · App | C · Informe / sustentación |
|---|---|---|---|
| Mañana | Arreglar lo que Postman/app encuentren. Correr `pruebe-humo.ps1` | Carrito → checkout → pedidos funcionando; capturas de cada pantalla | Completar el documento (sección 12: objetivos, marco teórico, metodología, conclusiones) |
| Tarde | Ensayar la demo del servidor; tener `docker compose down -v && docker compose up -d --build` probado desde cero | Ensayar la demo de la app; grabar un video corto de respaldo | Diagramas (casos de uso, ER, secuencia del pedido); guion y diapositivas |
| Noche | **Ensayo completo con los 3**, cronometrado | | Entrega: repositorio, documento, colección Postman |

## Demo sugerida (7 minutos)

1. `docker compose up -d --build` → mostrar contenedores (`docker ps`) y las tablas (`psql … \dt`). *(A)*
2. En la app: ver catálogo → agregar al carrito → iniciar sesión → confirmar pedido → ver "Mis pedidos". *(B)*
3. En Postman: correr el Runner y mostrar el verde; abrir la carpeta 5 y explicar los 403/401 (seguridad). *(C)*
4. En el panel web (`/admin/dashboard`, maría@flanbross.co): el pedido nuevo aparece; cambiarlo a *entregado* y ver la factura pagada. *(A)*
5. Cerrar con limitaciones conocidas (sección 11 del documento) y trabajo futuro.

## Plan B si algo falla
- **La base no levanta:** `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=memoria"` (sin base de datos).
- **La app no compila a tiempo:** demostrar con las páginas web (`http://localhost:8080`) y mostrar el código de la app.
- **Siempre:** video corto de respaldo y capturas del Runner de Postman.

## Qué NO tocar (salvo que sobre tiempo)
Cupones aplicados en el servidor · pantalla del repartidor en la app · fotos del catálogo · notificaciones.
