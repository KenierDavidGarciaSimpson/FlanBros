package com.flanbross.app.data

import android.content.Context
import com.flanbross.app.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

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

    fun urlImagen(ruta: String?): String? =
        ruta?.takeIf { it.isNotBlank() }?.let { urlBase.resolve(it.removePrefix("/"))?.toString() }
}
