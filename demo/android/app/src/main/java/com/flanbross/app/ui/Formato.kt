package com.flanbross.app.ui

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formato {
    private val colombia = Locale("es", "CO")
    private val patronFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    fun pesos(valor: Long): String = "$" + String.format(colombia, "%,d", valor)

    fun pesos(valor: Double): String = pesos(valor.toLong())

    fun fechaHora(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return runCatching { LocalDateTime.parse(iso.take(19)).format(patronFecha) }.getOrDefault(iso)
    }

    fun estadoPedido(estado: String): String = when (estado) {
        "pendiente" -> "Pendiente"
        "en_preparacion" -> "En preparación"
        "listo" -> "Listo"
        "entregado" -> "Entregado"
        "cancelado" -> "Cancelado"
        else -> estado
    }
}

object Cobro {
    const val COSTO_DOMICILIO = 7000L

    fun iva(subtotal: Long): Long = (subtotal * 8 + 50) / 100

    fun total(subtotal: Long): Long = subtotal + iva(subtotal)

    fun descuento(subtotal: Long, porcentaje: Int): Long = (subtotal * porcentaje + 50) / 100
}
