# App Android de FlanBross (Kotlin + Jetpack Compose)

Cliente móvil del servidor Spring Boot. Cubre el recorrido del cliente: catálogo, producto, carrito,
checkout, pedidos, cuenta, contacto, registro, inicio de sesión y recuperación de contraseña.

## Montaje en Android Studio

1. **Nuevo proyecto** → *Empty Activity* (Jetpack Compose). Nombre `FlanBross`, **paquete `com.flanbross.app`**,
   Kotlin, minSdk 26.
2. Borra del proyecto nuevo `MainActivity.kt` y la carpeta `ui/theme` que genera la plantilla.
3. Copia sobre el proyecto, respetando rutas:
   - `app/src/main/java/com/flanbross/app/**`  (todo el código)
   - `app/src/main/AndroidManifest.xml`  (reemplaza el generado; conserva tus íconos `@mipmap`)
   - `app/src/debug/AndroidManifest.xml`  (permite HTTP solo en debug)
4. **Gradle:** fusiona `app/build.gradle.kts` (de referencia) con el tuyo: plugin `serialization`, `buildConfig = true`,
   el campo `API_URL` y las dependencias. En el `build.gradle.kts` de la raíz agrega
   `id("org.jetbrains.kotlin.plugin.serialization") version "<tu versión de Kotlin>" apply false`.
5. *Sync Project with Gradle Files*.

## Conexión con el servidor

| Dónde corre la app | Dirección del servidor |
|---|---|
| Emulador de Android Studio | `http://10.0.2.2:8080/` (ya es el valor por defecto) |
| Celular real (misma red Wi-Fi que el PC) | En `gradle.properties`: `flanbross.apiUrl=http://IP-DE-TU-PC:8080/` |

- Arranca antes el servidor: `docker compose up -d --build` (todo en contenedores) o `docker compose up -d db` + `.\mvnw.cmd spring-boot:run`.
- Con celular real, el Firewall de Windows debe permitir el puerto 8080.
- Cuentas de prueba (clientes): `laura@demo.com` / `demo1234`. El personal (administrador, repostero, etc.)
  **no** puede entrar a esta app: usa el panel web.

## Estructura

| Carpeta | Contenido |
|---|---|
| `data/` | `ClienteHttp` (OkHttp + cookies + token CSRF), `Repositorio` (un método por endpoint), `Modelos` |
| `ui/` | ViewModels (`Sesion`, `Catalogo`, `Carrito`, `Pedidos`), pantallas Compose y `Navegacion` |
