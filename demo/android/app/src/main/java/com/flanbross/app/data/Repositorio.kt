package com.flanbross.app.data

import kotlinx.serialization.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

sealed interface Res<out T> {
    data class Ok<T>(val dato: T, val mensaje: String = "") : Res<T>
    data class Error(val mensaje: String) : Res<Nothing>
}

inline fun <reified T> leer(respuesta: Respuesta): T? =
    respuesta.datos?.let { runCatching { JsonApp.decodeFromJsonElement(serializer<T>(), it) }.getOrNull() }

class Repositorio(private val http: ClienteHttp) {

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

    suspend fun cerrarSesion() {
        http.enviar("POST", "api/logout")
        http.cookies.limpiar()
    }

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

    suspend fun validarCupon(codigo: String, subtotal: Long): Res<CuponValido> {
        val cuerpo = buildJsonObject { put("codigo", codigo); put("subtotal", subtotal) }
        val r = http.enviar("POST", "api/cupones", cuerpo.toString())
        if (!r.respuesta.exito) return Res.Error(r.respuesta.mensaje)
        return leer<CuponValido>(r.respuesta)?.let { Res.Ok(it, r.respuesta.mensaje) }
            ?: Res.Error("Respuesta inesperada del servidor")
    }

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
