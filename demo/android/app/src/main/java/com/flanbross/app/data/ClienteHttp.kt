package com.flanbross.app.data

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/** Configuracion de JSON compartida por toda la app. */
val JsonApp = Json {
    ignoreUnknownKeys = true   // el servidor puede agregar campos sin romper la app
    coerceInputValues = true
    explicitNulls = false
}

/** Codigo HTTP + sobre ya decodificado. codigo 0 = no hubo conexion. */
data class Resultado(val codigo: Int, val respuesta: Respuesta)

/**
 * ClienteHttp — habla con el servidor Spring Boot.
 *
 * Reproduce lo que hace el navegador:
 *  - Guarda y reenvia las cookies de sesion (AlmacenCookies).
 *  - En POST/PUT/DELETE agrega la cabecera X-XSRF-TOKEN con el valor de la cookie XSRF-TOKEN.
 *    Si todavia no la tiene, pide GET /api/sesion, que hace que el servidor la entregue.
 */
class ClienteHttp(private val urlBase: HttpUrl, val cookies: AlmacenCookies) {

    /** Se invoca cuando el servidor responde 401 a una llamada que no era de acceso (sesion vencida). */
    var alExpirar: (() -> Unit)? = null

    private val cliente: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookies)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { cadena -> agregarCsrf(cadena) })
        .build()

    private fun agregarCsrf(cadena: Interceptor.Chain): Response {
        var peticion = cadena.request()
        if (peticion.method != "GET" && peticion.method != "HEAD") {
            if (cookies.valor("XSRF-TOKEN") == null) {
                val previa = Request.Builder().url(urlBase.resolve("api/sesion")!!).get().build()
                cadena.proceed(previa).close()
            }
            cookies.valor("XSRF-TOKEN")?.let {
                peticion = peticion.newBuilder().header("X-XSRF-TOKEN", it).build()
            }
        }
        return cadena.proceed(peticion)
    }

    /** [ruta] es relativa al servidor, sin barra inicial: "api/productos?categoria=Tortas". */
    suspend fun enviar(metodo: String, ruta: String, cuerpo: String? = null): Resultado =
        withContext(Dispatchers.IO) {
            val url = urlBase.resolve(ruta) ?: return@withContext Resultado(0, Respuesta(false, "Ruta invalida"))
            val constructor = Request.Builder().url(url)
            val contenido = (cuerpo ?: "").toRequestBody("application/json; charset=utf-8".toMediaType())
            when (metodo) {
                "GET" -> constructor.get()
                "POST" -> constructor.post(contenido)
                "PUT" -> constructor.put(contenido)
                else -> throw IllegalArgumentException("Metodo no soportado: $metodo")
            }
            try {
                cliente.newCall(constructor.build()).execute().use { respuesta ->
                    val texto = respuesta.body?.string().orEmpty()
                    val sobre = if (texto.isBlank()) {
                        Respuesta(false, mensajePorCodigo(respuesta.code))
                    } else {
                        runCatching { JsonApp.decodeFromString(Respuesta.serializer(), texto) }
                            .getOrElse { Respuesta(false, mensajePorCodigo(respuesta.code)) }
                    }
                    if (respuesta.code == 401 && !esRutaDeAcceso(ruta)) alExpirar?.invoke()
                    Resultado(respuesta.code, sobre)
                }
            } catch (e: IOException) {
                Resultado(0, Respuesta(false, "No se pudo conectar con el servidor. Revisa tu conexión."))
            }
        }

    private fun esRutaDeAcceso(ruta: String) =
        ruta.startsWith("api/login") || ruta.startsWith("api/registro") ||
            ruta.startsWith("api/recuperar") || ruta.startsWith("api/sesion")

    private fun mensajePorCodigo(codigo: Int) = when (codigo) {
        401 -> "Tu sesión expiró. Inicia sesión de nuevo."
        403 -> "No tienes permiso para esta acción."
        429 -> "Demasiados intentos. Espera unos minutos."
        else -> "Error del servidor ($codigo)."
    }
}
