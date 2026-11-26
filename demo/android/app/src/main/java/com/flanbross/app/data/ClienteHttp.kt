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

val JsonApp = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

data class Resultado(val codigo: Int, val respuesta: Respuesta)

class ClienteHttp(private val urlBase: HttpUrl, val cookies: AlmacenCookies) {

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
