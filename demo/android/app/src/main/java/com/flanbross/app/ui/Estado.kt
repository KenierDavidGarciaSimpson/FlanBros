package com.flanbross.app.ui

/** Estado de una pantalla que carga una lista o dato desde el servidor. */
sealed interface Cargable<out T> {
    data object Cargando : Cargable<Nothing>
    data class Error(val mensaje: String) : Cargable<Nothing>
    data class Listo<T>(val datos: T) : Cargable<T>
}
