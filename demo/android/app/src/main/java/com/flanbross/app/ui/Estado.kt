package com.flanbross.app.ui

sealed interface Cargable<out T> {
    data object Cargando : Cargable<Nothing>
    data class Error(val mensaje: String) : Cargable<Nothing>
    data class Listo<T>(val datos: T) : Cargable<T>
}
