package com.flanbross.app.data

import android.content.Context
import com.flanbross.app.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Servicios — punto unico de acceso a la red (inyeccion manual de dependencias).
 * Se inicializa una sola vez en FlanBrossApp.onCreate().
 */
object Servicios {
    lateinit var urlBase: HttpUrl
        private set
    lateinit var http: ClienteHttp
        private set
    lateinit var repositorio: Repositorio
        private set

    fun iniciar(contexto: Context) {
        urlBase = BuildConfig.API_URL.toHttpUrl()
        http = ClienteHttp(urlBase, AlmacenCookies(contexto.applicationContext, urlBase))
        repositorio = Repositorio(http)
    }

    /** Convierte la ruta que guarda el servidor ("/img/productos/x.png") en una URL completa. */
    fun urlImagen(ruta: String?): String? =
        ruta?.takeIf { it.isNotBlank() }?.let { urlBase.resolve(it.removePrefix("/"))?.toString() }
}
