package com.flanbross.app.data

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class AlmacenCookies(contexto: Context, private val urlBase: HttpUrl) : CookieJar {

    private val prefs = contexto.getSharedPreferences("cookies_flanbross", Context.MODE_PRIVATE)
    private val guardadas = mutableListOf<Cookie>()

    init {
        prefs.getStringSet(CLAVE, emptySet())?.forEach { linea ->
            Cookie.parse(urlBase, linea)?.let { guardadas.add(it) }
        }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (nueva in cookies) {
            guardadas.removeAll { it.name == nueva.name && it.domain == nueva.domain && it.path == nueva.path }
            if (nueva.expiresAt > System.currentTimeMillis()) guardadas.add(nueva)
        }
        persistir()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val ahora = System.currentTimeMillis()
        guardadas.removeAll { it.expiresAt <= ahora }
        return guardadas.filter { it.matches(url) }
    }

    @Synchronized
    fun valor(nombre: String): String? = guardadas.firstOrNull { it.name == nombre }?.value

    @Synchronized
    fun limpiar() {
        guardadas.clear()
        persistir()
    }

    private fun persistir() {
        prefs.edit().putStringSet(CLAVE, guardadas.map { it.toString() }.toSet()).apply()
    }

    private companion object {
        const val CLAVE = "cookies"
    }
}
