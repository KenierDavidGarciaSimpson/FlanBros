package com.flanbross.app.data

import kotlinx.serialization.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Resultado de una operacion: dato o mensaje de error listo para mostrar al usuario. */
sealed interface Res<out T> {
    data class Ok<T>(val dato: T, val mensaje: String = "") : Res<T>
    data class Error(val mensaje: String) : Res<Nothing>
}

inline fun <reified T> leer(respuesta: Respuesta): T? =
    respuesta.datos?.let { runCatching { JsonApp.decodeFromJsonElement(serializer<T>(), it) }.getOrNull() }

/**
 * Repositorio — un metodo por cada endpoint de la API que usa la app.
 * Contrato completo en docs/app-movil.md del proyecto del servidor.
 */
class Repositorio(private val http: ClienteHttp) {

    // ---------------- Sesion ----------------

    /** Usuario de la sesion guardada, o null si no hay sesion (o el servidor no responde). */
    suspend fun sesionActual(): Usuario? {
        val r = http.enviar("GET", "api/sesion")
        return if (r.respuesta.exito) leer<DatosSesion>(r.respuesta)?.usuario else null
    }

    suspend fun login(correo: String, clave: String): Res<Usuario> {
        val cuerpo = buildJsonObject { put("correo", correo); put("contrasena", clave) }
        return usuarioDe(http.enviar("POST", "api/login", cuerpo.toString()))
    }

    suspend fun registro(
        nombre: String, apellido: String, correo: String, clave: String,
        telefono: String?, direccion: String?,
    ): Res<Usuario> {
        val cuerpo = buildJsonObject {
            put("nombre", nombre); put("apellido", apellido)
            put("correo", correo); put("contrasena", clave)
            if (!telefono.isNullOrBlank()) put("telefono", telefono)
            if (!direccion.isNullOrBlank()) put("direccion", direccion)
        }
        return usuarioDe(http.enviar("POST", "api/registro", cuerpo.toString()))
    }

    /** Cierra la sesion en el servidor y borra las cookies locales. */
    suspend fun cerrarSesion() {
        http.enviar("POST", "api/logout")
        http.cookies.limpiar()
    }

    /** Devuelve el token solo si el servidor esta en modo demo (sin correo); si no, null. */
    suspend fun solicitarRecuperacion(correo: String): Res<String?> {
        val cuerpo = buildJsonObject { put("correo", correo) }
        val r = http.enviar("POST", "api/recuperar", cuerpo.toString())
        if (!r.respuesta.exito) return Res.Error(r.respuesta.mensaje)
        val token = (r.respuesta.datos as? JsonObject)?.get("token")?.jsonPrimitive?.contentOrNull
        return Res.Ok(token, r.respuesta.mensaje)
    }

    suspend fun cambiarContrasena(token: String, nueva: String): Res<Unit> {
        val cuerpo = buildJsonObject { put("token", token); put("nueva_contrasena", nueva) }
        val r = http.enviar("POST", "api/recuperar", cuerpo.toString())
        return if (r.respuesta.exito) Res.Ok(Unit, r.respuesta.mensaje) else Res.Error(r.respuesta.mensaje)
    }

    // ---------------- Catalogo y pedidos ----------------

    suspend fun productos(): Res<List<Producto>> {
        val r = http.enviar("GET", "api/productos")
        if (!r.respuesta.exito) return Res.Error(r.respuesta.mensaje)
        return leer<List<Producto>>(r.respuesta)?.let { Res.Ok(it) } ?: Res.Error("Respuesta inesperada del servidor")
    }

    suspend fun pedidos(idCliente: Int): Res<List<Pedido>> {
        val r = http.enviar("GET", "api/pedidos?id_cliente=$idCliente")
        if (!r.respuesta.exito) return Res.Error(r.respuesta.mensaje)
        return leer<List<Pedido>>(r.respuesta)?.let { Res.Ok(it) } ?: Res.Error("Respuesta inesperada del servidor")
    }

    suspend fun crearPedido(pedido: NuevoPedido): Res<PedidoCreado> {
        val r = http.enviar("POST", "api/pedidos", JsonApp.encodeToString(NuevoPedido.serializer(), pedido))
        if (!r.respuesta.exito) return Res.Error(r.respuesta.mensaje)
        return leer<PedidoCreado>(r.respuesta)?.let { Res.Ok(it, r.respuesta.mensaje) }
            ?: Res.Error("Respuesta inesperada del servidor")
    }

    // ---------------- Contacto ----------------

    suspend fun enviarContacto(
        nombre: String, correo: String, telefono: String?, asunto: String, mensaje: String,
    ): Res<Unit> {
        val cuerpo = buildJsonObject {
            put("nombre", nombre); put("correo", correo)
            if (!telefono.isNullOrBlank()) put("telefono", telefono)
            put("asunto", asunto); put("mensaje", mensaje)
        }
        val r = http.enviar("POST", "api/contacto", cuerpo.toString())
        return if (r.respuesta.exito) Res.Ok(Unit, r.respuesta.mensaje) else Res.Error(r.respuesta.mensaje)
    }

    private fun usuarioDe(r: Resultado): Res<Usuario> {
        if (!r.respuesta.exito) return Res.Error(r.respuesta.mensaje)
        val usuario = leer<DatosSesion>(r.respuesta)?.usuario
            ?: return Res.Error("Respuesta inesperada del servidor")
        return Res.Ok(usuario, r.respuesta.mensaje)
    }
}
